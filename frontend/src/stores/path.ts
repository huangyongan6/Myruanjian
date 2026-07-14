import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LearningPath, PathData, PathStep } from '@/types/path'
import { getLatestPath, generatePath } from '@/services/path'
import { safeJsonParse } from '@/utils/format'

/**
 * 学习路径 Store。
 */
export const usePathStore = defineStore('path', () => {
  const currentPath = ref<LearningPath | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const steps = computed<PathStep[]>(() => {
    if (!currentPath.value?.pathData) return []
    const parsed = safeJsonParse<PathData>(currentPath.value.pathData, null)
    if (!parsed || !Array.isArray(parsed.steps)) return []
    return parsed.steps.map((step, index) => ({
      ...step,
      index: step.index ?? index,
      completed: step.completed ?? false
    }))
  })

  const currentStepIndex = computed(() => currentPath.value?.currentStep ?? 0)
  const progressPercent = computed(() => {
    if (!currentPath.value || currentPath.value.totalSteps === 0) return 0
    return Math.round((currentStepIndex.value / currentPath.value.totalSteps) * 100)
  })

  async function fetchLatest(studentId: number): Promise<LearningPath | null> {
    loading.value = true
    error.value = null
    try {
      currentPath.value = await getLatestPath(studentId)
      return currentPath.value
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载路径失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function generate(studentId: number): Promise<LearningPath | null> {
    loading.value = true
    error.value = null
    try {
      currentPath.value = await generatePath({ studentId })
      return currentPath.value
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '生成路径失败'
      return null
    } finally {
      loading.value = false
    }
  }

  function markStepComplete(index: number): void {
    if (!currentPath.value) return
    const next = Math.max(currentPath.value.currentStep, index + 1)
    currentPath.value.currentStep = next
  }

  function clear(): void {
    currentPath.value = null
  }

  return {
    currentPath,
    steps,
    currentStepIndex,
    progressPercent,
    loading,
    error,
    fetchLatest,
    generate,
    markStepComplete,
    clear
  }
})
