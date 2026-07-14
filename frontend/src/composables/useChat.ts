import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useStudentStore } from '@/stores/student'
import { useWebSocket } from './useWebSocket'
import { sendMessage as sendRest } from '@/services/chat'
import type { ChatResponse } from '@/types/chat'

/**
 * 对话组合式函数。
 *
 * <p>对应 CLAUDE.md §8：
 * <ul>
 *   <li>STOMP 订阅 /topic/chat/{studentId} 接收流式消息</li>
 *   <li>REST 发送 /api/chat/send（兜底，确保消息可达）</li>
 *   <li>通过 chatStore 维护消息列表与流式缓冲</li>
 * </ul>
 */
export function useChat() {
  const chatStore = useChatStore()
  const studentStore = useStudentStore()

  const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || '/ws'
  const ws = useWebSocket(wsBaseUrl)

  // 已订阅的学生 ID：避免每次 sendMessage 都重复调用 ws.subscribe，
  // 否则 STOMP 会注册多个订阅，回调被多次触发 → 流式内容被拼接 N 遍。
  const subscribedStudentId = ref<number | null>(null)

  const currentStudentId = computed(() => studentStore.currentStudentId)
  const messages = computed(() =>
    currentStudentId.value !== null ? chatStore.getMessages(currentStudentId.value) : []
  )
  const streaming = computed(() => chatStore.streamingBuffer)
  const isStreamingActive = computed(() => chatStore.isStreamingActive)

  function ensureConnected(): void {
    if (!ws.connected.value) {
      ws.connect()
    }
    if (currentStudentId.value !== null && subscribedStudentId.value !== currentStudentId.value) {
      // 同一学生只订阅一次；切换学生时重新订阅
      subscribedStudentId.value = currentStudentId.value
      ws.subscribe(`/topic/chat/${currentStudentId.value}`, (frame) => {
        try {
          const payload = JSON.parse(frame.body) as ChatResponse
          handleServerMessage(payload)
        } catch {
          // 静默忽略解析错误
        }
      })
    }
  }

  function handleServerMessage(payload: ChatResponse): void {
    if (currentStudentId.value === null) return
    if (payload.type === 'message') {
      chatStore.appendStreamChunk(currentStudentId.value, payload.content)
      chatStore.currentAgent = payload.agentType ?? payload.type
    } else if (payload.type === 'done') {
      chatStore.finalizeStream()
    } else if (payload.type === 'error') {
      ElMessage.error(payload.content || '对话出错')
      chatStore.finalizeStream()
    } else {
      // progress / resource 等其他类型
      chatStore.currentAgent = payload.type
    }
  }

  async function sendMessage(content: string): Promise<void> {
    const trimmed = content.trim()
    if (!trimmed) return
    if (currentStudentId.value === null) {
      ElMessage.warning('请先选择或创建学生')
      return
    }
    chatStore.appendMessage(currentStudentId.value, 'user', trimmed)
    try {
      ensureConnected()
      // 优先用 WebSocket 发送（流式）
      ws.send('/app/chat.send', {
        studentId: currentStudentId.value,
        content: trimmed
      })
    } catch {
      // WS 失败时降级到 REST
      const response = await sendRest({
        studentId: currentStudentId.value,
        content: trimmed
      })
      chatStore.appendMessage(currentStudentId.value, 'assistant', response.content)
    }
  }

  function disconnect(): void {
    subscribedStudentId.value = null
    ws.disconnect()
  }

  return {
    connected: ws.connected,
    reconnecting: ws.reconnecting,
    messages,
    streaming,
    isStreamingActive,
    sendMessage,
    ensureConnected,
    disconnect
  }
}
