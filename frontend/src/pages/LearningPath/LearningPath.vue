<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import PathTimeline from '@/components/PathTimeline/PathTimeline.vue'
import RecommendPanel from '@/components/RecommendPanel/RecommendPanel.vue'
import { usePathStore } from '@/stores/path'
import { useStudentStore } from '@/stores/student'
import { recommendResources } from '@/services/recommend'
import { mockRecommend } from '@/services/mockResources'
import type { RecommendedResource } from '@/types/recommend'

const router = useRouter()
const pathStore = usePathStore()
const studentStore = useStudentStore()

const studentId = computed(() => studentStore.currentStudentId)
const recommendations = ref<RecommendedResource[]>([])
const loadingRecommend = ref(false)

async function loadAll(): Promise<void> {
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  await pathStore.fetchLatest(studentId.value)
  await loadRecommend()
}

/**
 * 当前学习步骤的知识点（用于在推荐列表里高亮上下文关联的资源）。
 *
 * <p>优先取当前进行中步骤；无进行中步骤时回退到下一步；都没有则为空。
 */
const currentStepPoint = computed<string>(() => {
  const steps = pathStore.steps
  if (steps.length === 0) return ''
  const idx = pathStore.currentStepIndex
  const step = steps[idx] ?? steps[steps.length - 1]
  return step?.knowledgePoint ?? ''
})

/**
 * 加载推荐资源：优先从真实接口获取；失败或空则 fallback 到 mock 数据。
 *
 * <p>这样开发 / 演示阶段就算后端无数据，推荐栏也有可点击浏览的内容。
 */
async function loadRecommend(): Promise<void> {
  if (studentId.value === null) return
  loadingRecommend.value = true
  try {
    const real = await recommendResources(studentId.value, 6)
    if (real.length > 0) {
      recommendations.value = real
      return
    }
    // 真实接口成功但返回空列表：fallback 到 mock（且上下文匹配当前步骤）
    recommendations.value = mockRecommend(
      studentId.value,
      currentStepPoint.value,
      6
    ) as RecommendedResource[]
  } catch {
    // 真实接口失败：同样 fallback 到 mock
    recommendations.value = mockRecommend(
      studentId.value,
      currentStepPoint.value,
      6
    ) as RecommendedResource[]
  } finally {
    loadingRecommend.value = false
  }
}

async function generate(): Promise<void> {
  if (studentId.value === null) return
  try {
    const result = await pathStore.generate(studentId.value)
    if (result) {
      ElMessage.success('学习路径已生成')
    }
    // 重新从后端获取最新数据，确保路径内容是最新的
    await pathStore.fetchLatest(studentId.value)
    await loadRecommend()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '生成学习路径失败'
    if (msg.includes('画像')) {
      ElMessage.warning('请先前往「对话学习」页面与 AI 助手交流，完成学习画像构建后再生成学习路径')
    } else {
      ElMessage.error(msg)
    }
  }
}

function onStepComplete(index: number): void {
  pathStore.markStepComplete(index)
}

function onViewResource(resourceId: number): void {
  router.push({ name: 'ResourceCenter', query: { resourceId: String(resourceId) } })
}

watch(studentId, () => {
  if (studentId.value !== null) loadAll()
})

onMounted(loadAll)
</script>

<template>
  <div class="page-container learning-path">
    <div class="learning-path__actions">
      <el-button type="primary" :loading="pathStore.loading" @click="generate">
        <IconSvg name="compass" :size="16" /> 重新规划学习路径
      </el-button>
      <el-button @click="loadRecommend"><IconSvg name="refresh" :size="16" /> 刷新推荐</el-button>
    </div>

    <div v-if="!pathStore.currentPath && !pathStore.loading" class="learning-path__empty">
      <div class="learning-path__empty-icon">
        <IconSvg name="compass" :size="48" />
      </div>
      <h3 class="learning-path__empty-title">暂无学习路径</h3>
      <p class="learning-path__empty-desc">学习路径需要基于您的学习画像来智能规划</p>
      <div class="learning-path__empty-tips">
        <p><IconSvg name="chat" :size="16" /> 请先前往「对话学习」页面与 AI 助手交流</p>
        <p><IconSvg name="brain" :size="16" /> AI 将根据您的对话自动构建学习画像</p>
        <p><IconSvg name="path" :size="16" /> 画像构建完成后即可生成个性化学习路径</p>
      </div>
      <el-button type="primary" @click="router.push('/chat')">
        <IconSvg name="arrow-right" :size="16" /> 去对话学习
      </el-button>
    </div>

    <template v-else>
      <el-card v-if="pathStore.currentPath" class="learning-path__progress" shadow="never">
        <el-progress
          :percentage="pathStore.progressPercent"
          :status="pathStore.progressPercent >= 100 ? 'success' : ''"
          :stroke-width="18"
        />
        <div class="learning-path__progress-text">
          进度：第 {{ pathStore.currentStepIndex }} / {{ pathStore.currentPath.totalSteps }} 步
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :md="16">
          <h3 class="learning-path__section-title"><IconSvg name="location" :size="16" /> 学习步骤</h3>
          <PathTimeline
            :steps="pathStore.steps"
            :current-index="pathStore.currentStepIndex"
            @complete="onStepComplete"
          />
        </el-col>
        <el-col :xs="24" :md="8">
          <h3 class="learning-path__section-title"><IconSvg name="target" :size="16" /> 推荐资源</h3>
          <div v-if="loadingRecommend" class="empty-tip">加载中...</div>
          <RecommendPanel
            v-else
            :items="recommendations"
            :current-point="currentStepPoint"
            empty-text="暂无推荐资源"
            @view="onViewResource"
          />
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped lang="scss">
.learning-path {
  &__actions {
    display: flex;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
  }
  &__progress {
    margin-bottom: $spacing-lg;
    border-radius: $radius-lg;
    border: 1px solid $border-light;
    padding: $spacing-lg;

    :deep(.el-progress__bar) {
      border-radius: $radius-full;
    }
  }
  &__progress-text {
    margin-top: $spacing-md;
    font-size: 14px;
    color: $text-secondary;
    text-align: right;
    font-weight: 500;
  }
  &__section-title {
    font-size: 18px;
    font-weight: 700;
    margin: 0 0 $spacing-lg;
    color: $text-primary;
    letter-spacing: -0.01em;
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    &::before {
      content: '';
      width: 4px;
      height: 18px;
      border-radius: 2px;
      background: $primary-color;
    }
  }
  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: $spacing-2xl * 2;
    text-align: center;
  }
  &__empty-icon {
    width: 80px;
    height: 80px;
    border-radius: $radius-full;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: $spacing-lg;
    color: $primary-color;
  }
  &__empty-title {
    font-size: 20px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 $spacing-sm;
  }
  &__empty-desc {
    font-size: 14px;
    color: $text-secondary;
    margin: 0 0 $spacing-xl;
  }
  &__empty-tips {
    text-align: left;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-xl;
    border: 1px solid $border-light;

    p {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      font-size: 13px;
      color: $text-regular;
      margin: $spacing-xs 0;

      svg {
        color: $primary-color;
      }
    }
  }
}
</style>
