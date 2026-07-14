<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import ChatMessage from '@/components/ChatMessage/ChatMessage.vue'
import { useStudentStore } from '@/stores/student'
import { useChatStore } from '@/stores/chat'
import { sendMessage as sendRest } from '@/services/chat'

const studentStore = useStudentStore()
const chatStore = useChatStore()

const input = ref('')
const sending = ref(false)
const imageUrl = ref('')
const scrollRef = ref<HTMLDivElement | null>(null)
// 用户主动向上滚动查看历史时，禁止自动滚到底；只有新发送消息或首次进入时才回到底部。
const stickyToBottom = ref(true)
// 内容变化监听器：只要"贴底"就持续滚到底，应对 markdown / emoji 异步渲染
let contentObserver: MutationObserver | null = null
// 滚动容器是否"已就绪"。在就绪前，整块区域保持 visibility: hidden，避免
// 浏览器先把滚动条呈现在顶部、我们再把它推到最底所造成的"自上而下再瞬间
// 跳到底"视觉跳动。一旦 scrollHeight 稳定，就立即标记为就绪。
const ready = ref(false)

const studentId = computed(() => studentStore.currentStudentId)
const messages = computed(() => (studentId.value !== null ? chatStore.getMessages(studentId.value) : []))
const hasMore = computed(() => (studentId.value !== null ? chatStore.hasMore[studentId.value] !== false : false))
const loadingMore = computed(() =>
  studentId.value !== null ? !!chatStore.loadingMore[studentId.value] : false
)

/**
 * 滚动到底部：用于首次加载完成、用户发送后。
 *
 * <p>不限制固定帧数，而是持续尝试直到 scrollHeight 连续若干次不再增长——
 * 这是因为 Markdown / 代码高亮是异步渲染的（highlight.js 等），固定 N 帧
 * 经常不足以等到 DOM 完全铺开。最多 {@link STICKY_MAX_FRAMES} 帧兜底，
 * 避免极端情况下死循环。
 *
 * <p>第一次把滚动容器收窄到不可见区域并贴到底部后，
 * 通过 {@code onSettled} 回调（默认）通知调用方暴露出来，避免浏览器先布局
 * 到顶部再被推到最底。
 */
const STICKY_MAX_FRAMES = 60

function scrollToBottom(onSettled?: () => void): void {
  const el = scrollRef.value
  if (!el) {
    onSettled?.()
    return
  }
  let lastHeight = -1
  let stableFrames = 0
  let attempts = 0
  let settled = false
  const finish = (): void => {
    if (settled) return
    settled = true
    onSettled?.()
  }
  const tryScroll = (): void => {
    const target = scrollRef.value
    if (!target || settled) return
    target.scrollTop = target.scrollHeight
    attempts += 1
    const currentHeight = target.scrollHeight
    if (currentHeight === lastHeight) {
      stableFrames += 1
    } else {
      stableFrames = 0
      lastHeight = currentHeight
    }
    if (stableFrames >= 2) {
      finish()
      return
    }
    if (attempts >= STICKY_MAX_FRAMES) {
      finish()
      return
    }
    requestAnimationFrame(tryScroll)
  }
  tryScroll()
}

/**
 * 安装内容变化监听器。
 *
 * <p>只要 stickyToBottom 为真，列表内任何变化（消息追加、markdown 异步渲染、emoji
 * 占位替换等）都立即把 scrollTop 推到最底，避免"进入时显示一半，要手动往下滑才看到最新"。
 * 离开页面时务必断开。
 */
function setupContentObserver(): void {
  contentObserver?.disconnect()
  const el = scrollRef.value
  if (!el) return
  contentObserver = new MutationObserver(() => {
    if (!stickyToBottom.value) return
    const target = scrollRef.value
    if (target) target.scrollTop = target.scrollHeight
  })
  contentObserver.observe(el, {
    childList: true,
    subtree: true,
    characterData: true
  })
}

/**
 * 监听滚动事件：
 * 1. 用户已经接近底部（距底 < 32px）→ 进入/保持"贴底"模式；
 * 2. 用户滚到顶部（scrollTop <= 0）且还有更早历史 → 触发向上加载，并在加载完成后维持视觉位置。
 */
async function onScroll(): Promise<void> {
  const el = scrollRef.value
  if (!el) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  stickyToBottom.value = distanceToBottom <= 32

  if (el.scrollTop <= 0 && studentId.value !== null && hasMore.value && !loadingMore.value) {
    // 记录加载前高度，加载完成后把 scrollTop 同步回原位置，避免视觉跳动
    const prevScrollHeight = el.scrollHeight
    const prevScrollTop = el.scrollTop
    const loaded = await chatStore.loadMoreHistory(studentId.value)
    if (loaded) {
      nextTick(() => {
        if (!scrollRef.value) return
        const delta = scrollRef.value.scrollHeight - prevScrollHeight
        scrollRef.value.scrollTop = prevScrollTop + delta
      })
    } else if (scrollRef.value) {
      // 没有更早历史：把滚动位置稍微回拉一点，避免卡在 0
      scrollRef.value.scrollTop = 1
    }
  }
}

async function send(): Promise<void> {
  const text = input.value.trim()
  if (!text) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  sending.value = true
  try {
    const content = imageUrl.value
      ? `${text}\n\n[图片] ${imageUrl.value}`
      : text
    chatStore.appendMessage(studentId.value, 'user', content, 'tutor')
    const response = await sendRest({ studentId: studentId.value, content })
    chatStore.appendMessage(studentId.value, 'assistant', response.content, response.type)
    input.value = ''
    imageUrl.value = ''
    stickyToBottom.value = true
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

async function clearAll(): Promise<void> {
  if (studentId.value === null) return
  chatStore.clearHistory(studentId.value)
  stickyToBottom.value = true
  scrollToBottom()
}

/**
 * 切换学生时 / 首次进入页面时：
 * 1. 先把贴底标志复位，等异步加载完成后再贴底；
 * 2. 异步拉取最新一页历史（store 内部已经处理排序：最新在列表末尾）。
 */
async function loadHistoryForCurrent(): Promise<void> {
  if (studentId.value === null) return
  // 学生切换属于运行时切换，滚动容器应保持可见；只有真正的"首次进入"
  // 才需要走 ready 通道（在 onMounted 中显式把 ready 重置为 false）。
  stickyToBottom.value = true
  await chatStore.loadHistory(studentId.value)
  scrollToBottom()
}

watch(studentId, () => {
  loadHistoryForCurrent()
})

/**
 * 首屏贴底流程。
 *
 * <ol>
 *   <li>先把滚动容器隐藏（避免浏览器先布局到顶部、再被推到最底）；</li>
 *   <li>如果本地有缓存，先等 nextTick 让缓存消息渲染完并贴底一次；</li>
 *   <li>异步拉服务端最新一页历史，期间持续贴底；</li>
 *   <li>scrollHeight 稳定后，再把滚动容器显示出来——用户看到时就已在底部。</li>
 * </ol>
 */
async function settleOnFirstMount(): Promise<void> {
  ready.value = false
  if (studentId.value !== null) {
    stickyToBottom.value = true
    await nextTick()
    scrollToBottom()
    await chatStore.loadHistory(studentId.value)
    scrollToBottom(() => {
      ready.value = true
    })
  } else {
    await nextTick()
    scrollToBottom(() => {
      ready.value = true
    })
  }
}

onMounted(() => {
  setupContentObserver()
  settleOnFirstMount()
})

onBeforeUnmount(() => {
  contentObserver?.disconnect()
  contentObserver = null
  if (studentId.value !== null) {
    delete chatStore.loadingMore[studentId.value]
  }
})
</script>

<template>
  <div class="page-container tutor-page">
    <h2 class="page-title">智能辅导</h2>
    <p class="page-subtitle">遇到学习难题？直接向 AI 辅导老师提问，支持文字 + 图片链接。</p>

    <el-card class="tutor-page__card" shadow="never">
      <div
        ref="scrollRef"
        class="tutor-page__messages"
        :class="{ 'tutor-page__messages--hidden': !ready }"
        @scroll="onScroll"
      >
        <div v-if="loadingMore" class="tutor-page__loading-more">加载更早的消息...</div>
        <div v-if="messages.length === 0" class="empty-tip">
          🤖 辅导老师已就位。<br />
          描述你的问题（可附图片 URL），老师会逐步引导你思考。
        </div>
        <ChatMessage v-for="(m, idx) in messages" :key="`${m.createdAt ?? idx}-${idx}`" :message="m" />
        <div v-if="!hasMore && messages.length > 0" class="tutor-page__history-end">— 没有更早的消息了 —</div>
      </div>

      <div class="tutor-page__composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          :disabled="sending"
          placeholder="向辅导老师提问..."
        />
        <div class="tutor-page__composer-row">
          <el-input v-model="imageUrl" placeholder="（可选）图片 URL" />
          <el-button :loading="sending" type="primary" @click="send">提问</el-button>
          <el-button @click="clearAll">清空</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.tutor-page {
  &__card {
    display: flex;
    flex-direction: column;
    height: calc(100vh - #{$header-height} - 120px);
  }
  &__messages {
    flex: 1;
    overflow-y: auto;
    padding: $spacing-md 0;
    background: $bg-page;
    border-radius: $radius-md;
    margin-bottom: $spacing-md;
  }
  // 首屏贴底完成前：保留布局占位但不参与渲染 / 滚动事件，
  // 让"先把滚动条回到顶部、再被推到最底"那一段过程对用户不可见，
  // 视觉上即"页面打开直接在底部"。
  &__messages--hidden {
    visibility: hidden;
  }
  &__loading-more {
    text-align: center;
    color: $text-secondary;
    font-size: 12px;
    padding: $spacing-sm 0;
  }
  &__history-end {
    text-align: center;
    color: $text-secondary;
    font-size: 12px;
    padding: $spacing-md 0;
  }
  &__composer {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }
  &__composer-row {
    display: flex;
    gap: $spacing-sm;
    .el-input { flex: 1; }
  }
}
</style>
