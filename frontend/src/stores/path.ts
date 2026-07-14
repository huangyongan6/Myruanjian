import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { LearningPath, PathData, PathStep } from '@/types/path'
import { getLatestPath, generatePath, updatePathCurrentStep } from '@/services/path'
import { safeJsonParse } from '@/utils/format'
import { getItem, setItem, STORAGE_KEYS } from '@/utils/storage'

/**
 * 学习路径 Store。
 */
export const usePathStore = defineStore('path', () => {
  const currentPath = ref<LearningPath | null>(getItem<LearningPath>(STORAGE_KEYS.LEARNING_PATH))
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

  // 持久化到 localStorage：currentPath 变化时自动同步
  watch(currentPath, (path) => {
    if (path) {
      setItem(STORAGE_KEYS.LEARNING_PATH, path)
    } else {
      localStorage.removeItem(STORAGE_KEYS.LEARNING_PATH)
    }
  }, { immediate: true, deep: true })

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
    // 异步同步到后端持久化，进度更新不阻塞 UI
    const studentId = currentPath.value.studentId
    updatePathCurrentStep(studentId, next).catch((e) => {
      console.warn('[PathStore] markStepComplete 同步失败:', e)
    })
  }

  function clear(): void {
    currentPath.value = null
    localStorage.removeItem(STORAGE_KEYS.LEARNING_PATH)
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
