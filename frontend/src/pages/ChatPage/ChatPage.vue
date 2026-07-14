<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessage from '@/components/ChatMessage/ChatMessage.vue'
import { useChat } from '@/composables/useChat'
import { useStudentStore } from '@/stores/student'
import { useChatStore } from '@/stores/chat'

const studentStore = useStudentStore()
const chatStore = useChatStore()
const { messages, streaming, isStreamingActive, sendMessage, connected, ensureConnected, reconnecting } = useChat()

const input = ref('')
const sending = ref(false)
const scrollRef = ref<HTMLDivElement | null>(null)
const showInitDialog = ref(!studentStore.isLoggedIn)
const initName = ref('')
const initId = ref<number | null>(null)
const initMode = ref<'create' | 'load'>('create')
/** 用户是否主动滑动过页面（主动滑动后暂停自动滚动，直到发送新消息） */
const userScrolled = ref(false)

const currentStudent = computed(() => studentStore.currentStudent)
const currentStudentId = computed(() => studentStore.currentStudentId)

function scrollToBottom(): void {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

/**
 * 检测用户是否主动滑动：如果是则暂停自动滚动。
 * 用户发送新消息后重置为 false，恢复自动滚动。
 */
function onScroll(): void {
  if (!scrollRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = scrollRef.value
  // scrollTop < scrollHeight - clientHeight - 50 表示用户向上滑了（未在底部）
  const isAtBottom = scrollTop >= scrollHeight - clientHeight - 50
  if (!isAtBottom) {
    userScrolled.value = true
  } else {
    // 用户滑动回底部，视为已看完最新内容，重置标志
    userScrolled.value = false
  }
}

async function handleSend(): Promise<void> {
  const text = input.value.trim()
  if (!text) return
  sending.value = true
  // 用户发送消息后重置主动滑动标志，发送过程中自动滚动
  userScrolled.value = false
  // 如果 AI 正在输出，先终止当前输出
  if (isStreamingActive.value) {
    chatStore.cancelStream()
  }
  try {
    await sendMessage(text)
    input.value = ''
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

function handleKeydown(e: KeyboardEvent | Event): void {
  const evt = e as KeyboardEvent
  if (evt.key === 'Enter' && !evt.shiftKey) {
    evt.preventDefault()
    handleSend()
  }
}

async function confirmInit(): Promise<void> {
  if (initMode.value === 'create') {
    if (!initName.value.trim()) {
      ElMessage.warning('请输入学生姓名')
      return
    }
    const student = await studentStore.createAndSet(initName.value.trim())
    if (student) {
      showInitDialog.value = false
      ElMessage.success(`已创建学生：${student.name}`)
    }
  } else {
    if (!initId.value || initId.value <= 0) {
      ElMessage.warning('请输入有效的学生 ID')
      return
    }
    const student = await studentStore.loadById(initId.value)
    if (student) {
      showInitDialog.value = false
      ElMessage.success(`已加载学生：${student.name}`)
    } else {
      ElMessage.error('学生不存在')
    }
  }
}

async function clearHistory(): Promise<void> {
  if (currentStudentId.value === null) return
  try {
    await ElMessageBox.confirm('确认清空当前学生的对话历史？', '提示', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })
    chatStore.clearHistory(currentStudentId.value)
    ElMessage.success('已清空')
  } catch {
    // 取消
  }
}

async function loadHistory(): Promise<void> {
  if (currentStudentId.value === null) return
  await chatStore.loadHistory(currentStudentId.value)
  scrollToBottom()
}

const streamingMessage = computed(() => {
  if (!streaming.value) return null
  return {
    studentId: streaming.value.studentId,
    role: 'assistant' as const,
    content: streaming.value.content,
    agentType: streaming.value.agentType,
    createdAt: new Date().toISOString()
  }
})

/**
 * 监听消息变化自动滚动：
 * - 流式输出期间持续滚动（streamingBuffer 有内容时持续触发）
 * - 非流式时，只有用户没有主动滑动才自动滚动
 *
 * <p>注意：这里用 streamingMessage?.content 做深度监听，因为 streamingBuffer 对象
 * 引用不变但 content 字符串在不断拼接，只有监听 content 才能捕获每次追加。
 */
watch(
  () => [messages.value.length, streamingMessage.value?.content] as const,
  () => {
    if (streamingMessage.value || !userScrolled.value) {
      scrollToBottom()
    }
  }
)

onMounted(() => {
  if (studentStore.isLoggedIn) {
    ensureConnected()
    loadHistory()
  }
})
</script>

<template>
  <div class="page-container chat-page">
    <h2 class="page-title">对话学习</h2>
    <p class="page-subtitle">与 AI 助手对话，逐步构建你的学习画像，并触发个性化资源生成。</p>

    <div class="chat-page__toolbar">
      <el-tag v-if="connected" type="success" effect="plain">● 已连接</el-tag>
      <el-tag v-else-if="reconnecting" type="warning" effect="plain">● 重连中...</el-tag>
      <el-tag v-else type="info" effect="plain">● 未连接</el-tag>
      <div class="chat-page__actions">
        <el-button size="small" @click="loadHistory">加载历史</el-button>
        <el-button size="small" type="danger" plain @click="clearHistory">清空对话</el-button>
      </div>
    </div>

    <div ref="scrollRef" class="chat-page__messages" @scroll="onScroll">
      <div v-if="messages.length === 0 && !streamingMessage" class="empty-tip">
        👋 欢迎，{{ currentStudent?.name ?? '同学' }}！<br />
        告诉我你的学习背景、目标与困惑，我会为你推荐个性化资源。
      </div>

      <ChatMessage v-for="(msg, idx) in messages" :key="`${msg.createdAt ?? idx}`" :message="msg" />

      <ChatMessage v-if="streamingMessage" :message="streamingMessage" :streaming="true" />
    </div>

    <div class="chat-page__composer">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        :disabled="sending"
        placeholder="输入消息后回车发送，Shift+Enter 换行"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :loading="sending"
        :disabled="!input.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>

    <el-dialog v-model="showInitDialog" title="欢迎使用智能学习助手" width="420px" :show-close="false" :close-on-click-modal="false">
      <p class="chat-page__dialog-tip">请先选择或创建学生，才能开始对话。</p>
      <el-radio-group v-model="initMode" class="chat-page__dialog-mode">
        <el-radio-button value="create">创建新学生</el-radio-button>
        <el-radio-button value="load">加载已有学生</el-radio-button>
      </el-radio-group>
      <div class="chat-page__dialog-form">
        <el-input v-if="initMode === 'create'" v-model="initName" placeholder="输入学生姓名" />
        <el-input-number
          v-else
          v-model="initId"
          :min="1"
          placeholder="输入学生 ID"
          style="width: 100%"
        />
      </div>
      <template #footer>
        <el-button type="primary" :loading="studentStore.loading" @click="confirmInit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - #{$header-height});
  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
  }
  &__messages {
    flex: 1;
    overflow-y: auto;
    background: $bg-card;
    border-radius: $radius-md;
    border: 1px solid $border-lighter;
    padding: $spacing-md 0;
  }
  &__composer {
    margin-top: $spacing-md;
    display: flex;
    gap: $spacing-sm;
    align-items: flex-end;
    .el-textarea { flex: 1; }
  }
  &__dialog-tip {
    color: $text-regular;
    margin-bottom: $spacing-md;
  }
  &__dialog-mode {
    margin-bottom: $spacing-md;
  }
  &__dialog-form {
    margin-top: $spacing-md;
  }
}
</style>
