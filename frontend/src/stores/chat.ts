import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage, ChatRole } from '@/types/chat'
import { getHistory } from '@/services/chat'
import { getItem, setItem, STORAGE_KEYS } from '@/utils/storage'

interface StreamBuffer {
  studentId: number
  content: string
  agentType?: string
  startedAt: number
}

type MessageMap = Record<number, ChatMessage[]>

/** 单页历史消息条数：与后端 history 接口默认 limit 一致 */
const HISTORY_PAGE_SIZE = 20

/**
 * 对话 Store。
 *
 * <p>职责：管理按 studentId 隔离的消息列表、流式输出缓冲、当前 Agent 类型。
 *
 * <p>历史消息采用"向上加载"模式：首次只拉取最新 {@link HISTORY_PAGE_SIZE} 条，
 * 用户滚动到顶部时再以更早的时间戳为游标分页加载；store 负责
 * 维护 {@code hasMore}、{@code loadingMore} 等状态。
 */
export const useChatStore = defineStore('chat', () => {
  const messages = ref<MessageMap>(getItem<MessageMap>(STORAGE_KEYS.CHAT_HISTORY) ?? {})
  const streamingBuffer = ref<StreamBuffer | null>(null)
  const currentAgent = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  /** 流式输出是否处于进行中状态（用户可主动终止） */
  const isStreamingActive = ref(false)

  // 每个学生独立的"是否还有更早历史"标记。true / undefined 都视为可能还有。
  const hasMore = ref<Record<number, boolean>>({})
  // 每个学生独立的"正在向上加载历史"标记，避免重复触发
  const loadingMore = ref<Record<number, boolean>>({})

  function persistMessages(): void {
    setItem(STORAGE_KEYS.CHAT_HISTORY, messages.value)
  }

  function getMessages(studentId: number): ChatMessage[] {
    if (!messages.value[studentId]) {
      messages.value[studentId] = []
    }
    return messages.value[studentId] as ChatMessage[]
  }

  function appendMessage(studentId: number, role: ChatRole, content: string, agentType?: string): ChatMessage {
    const list = getMessages(studentId)
    const msg: ChatMessage = {
      studentId,
      role,
      content,
      agentType,
      createdAt: new Date().toISOString()
    }
    list.push(msg)
    persistMessages()
    return msg
  }

  function appendStreamChunk(studentId: number, chunk: string, agentType?: string): void {
    if (!streamingBuffer.value || streamingBuffer.value.studentId !== studentId) {
      streamingBuffer.value = {
        studentId,
        content: chunk,
        agentType,
        startedAt: Date.now()
      }
      isStreamingActive.value = true
    } else {
      streamingBuffer.value.content += chunk
      if (agentType) streamingBuffer.value.agentType = agentType
    }
  }

  function finalizeStream(): ChatMessage | null {
    if (!streamingBuffer.value) return null
    const buffer = streamingBuffer.value
    const msg = appendMessage(buffer.studentId, 'assistant', buffer.content, buffer.agentType)
    streamingBuffer.value = null
    currentAgent.value = null
    isStreamingActive.value = false
    return msg
  }

  /**
   * 取消正在进行的流式输出：将当前流式内容直接追加为用户消息，
   * 相当于用户主动中断 AI 回答并发送了新消息。
   */
  function cancelStream(): void {
    if (!streamingBuffer.value) return
    const buffer = streamingBuffer.value
    // 把 AI 未完成的回答追加到用户消息（表示用户中断了 AI 并继续提问）
    streamingBuffer.value = null
    currentAgent.value = null
    isStreamingActive.value = false
    // 不调用 appendMessage，仅仅是清空流式缓冲
  }

  /**
   * 首次加载某学生的最新历史。
   *
   * <p>用 {@link HISTORY_PAGE_SIZE} 作为分页大小；不足一页即视为到底。
   */
  async function loadHistory(studentId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const history = await getHistory(studentId, HISTORY_PAGE_SIZE)
      // 后端按 createdAt 倒序返回（最新在前），UI 期望按时间正序展示（最新在末尾）
      const sorted = [...history].sort((a, b) => {
        const ta = a.createdAt ? Date.parse(a.createdAt) : 0
        const tb = b.createdAt ? Date.parse(b.createdAt) : 0
        return ta - tb
      })
      messages.value[studentId] = sorted
      hasMore.value[studentId] = history.length >= HISTORY_PAGE_SIZE
      persistMessages()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载历史失败'
    } finally {
      loading.value = false
    }
  }

  /**
   * 向上加载更早的历史。
   *
   * <p>以当前列表最早一条消息的 createdAt 为游标，向后端请求更早的一页；
   * 拿到后做"前向拼接"（在 list 头部追加），由调用方负责维持滚动位置。
   *
   * @returns 是否真的拉到了更早的消息（false 表示没有更早历史或正在加载）
   */
  async function loadMoreHistory(studentId: number): Promise<boolean> {
    if (studentId === null || studentId === undefined) return false
    if (loadingMore.value[studentId]) return false
    if (hasMore.value[studentId] === false) return false

    const list = getMessages(studentId)
    const earliest = list[0]
    if (!earliest || !earliest.createdAt) {
      hasMore.value[studentId] = false
      return false
    }

    loadingMore.value[studentId] = true
    try {
      const older = await getHistory(studentId, HISTORY_PAGE_SIZE, earliest.createdAt)
      if (older.length === 0) {
        hasMore.value[studentId] = false
        return false
      }
      // 后端按 createdAt 倒序返回，UI 期望时间正序；倒序后拼接在 list 头部
      const asc = [...older].sort((a, b) => {
        const ta = a.createdAt ? Date.parse(a.createdAt) : 0
        const tb = b.createdAt ? Date.parse(b.createdAt) : 0
        return ta - tb
      })
      messages.value[studentId] = [...asc, ...list]
      // 不足一页：没有更早历史了
      if (older.length < HISTORY_PAGE_SIZE) {
        hasMore.value[studentId] = false
      }
      persistMessages()
      return true
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载更早历史失败'
      return false
    } finally {
      loadingMore.value[studentId] = false
    }
  }

  function clearHistory(studentId: number): void {
    messages.value[studentId] = []
    hasMore.value[studentId] = true
    delete loadingMore.value[studentId]
    persistMessages()
  }

  return {
    messages,
    streamingBuffer,
    currentAgent,
    loading,
    error,
    hasMore,
    loadingMore,
    isStreamingActive,
    getMessages,
    appendMessage,
    appendStreamChunk,
    finalizeStream,
    cancelStream,
    loadHistory,
    loadMoreHistory,
    clearHistory
  }
})
