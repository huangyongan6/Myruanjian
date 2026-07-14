<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
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
    const real = await recommendResources(studentId.value, 8)
    if (real.length > 0) {
      recommendations.value = real
      return
    }
    // 真实接口成功但返回空列表：fallback 到 mock（且上下文匹配当前步骤）
    recommendations.value = mockRecommend(
      studentId.value,
      currentStepPoint.value,
      8
    ) as RecommendedResource[]
  } catch {
    // 真实接口失败：同样 fallback 到 mock
    recommendations.value = mockRecommend(
      studentId.value,
      currentStepPoint.value,
      8
    ) as RecommendedResource[]
  } finally {
    loadingRecommend.value = false
  }
}

async function generate(): Promise<void> {
  if (studentId.value === null) return
  const result = await pathStore.generate(studentId.value)
  if (result) {
    ElMessage.success('学习路径已生成')
  }
  // 重新从后端获取最新数据，确保路径内容是最新的
  await pathStore.fetchLatest(studentId.value)
  await loadRecommend()
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
    <h2 class="page-title">学习路径</h2>
    <p class="page-subtitle">基于你的学习画像与历史记录，AI 智能规划的学习步骤序列。</p>

    <div class="learning-path__actions">
      <el-button type="primary" :loading="pathStore.loading" @click="generate">
        🧭 重新规划学习路径
      </el-button>
      <el-button @click="loadRecommend">🔄 刷新推荐</el-button>
    </div>

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
        <h3 class="learning-path__section-title">📍 学习步骤</h3>
        <PathTimeline
          :steps="pathStore.steps"
          :current-index="pathStore.currentStepIndex"
          @complete="onStepComplete"
        />
      </el-col>
      <el-col :xs="24" :md="8">
        <h3 class="learning-path__section-title">🎯 推荐资源</h3>
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
  }
  &__progress-text {
    margin-top: $spacing-sm;
    font-size: 13px;
    color: $text-secondary;
    text-align: right;
  }
  &__section-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 $spacing-md;
  }
}
</style>
