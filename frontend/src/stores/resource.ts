import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  LearningResource,
  ResourceType,
  GenerateResourceRequest
} from '@/types/resource'
import type { DocContent, MindMapContent, QuizContent, ReadingContent, CodeContent } from '@/types/resource'
import { listResources, getResourceById, generateResource } from '@/services/resource'

export interface GenerationProgress {
  current: number
  total: number
  label: string
  active: boolean
}

/**
 * 资源 Store。
 */
export const useResourceStore = defineStore('resource', () => {
  const resources = ref<LearningResource[]>([])
  const currentResource = ref<LearningResource | null>(null)
  const generationProgress = ref<GenerationProgress | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(studentId: number, type?: ResourceType): Promise<void> {
    loading.value = true
    error.value = null
    try {
      resources.value = await listResources(studentId, type)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载资源失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id: number): Promise<LearningResource | null> {
    loading.value = true
    error.value = null
    try {
      const data = await getResourceById(id)
      currentResource.value = data
      return data
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载资源详情失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function generate(payload: GenerateResourceRequest): Promise<LearningResource | null> {
    loading.value = true
    error.value = null
    generationProgress.value = {
      current: 1,
      total: 3,
      label: '正在调起 Agent...',
      active: true
    }
    try {
      const created = await generateResource(payload)
      resources.value = [created, ...resources.value]
      generationProgress.value = {
        current: 3,
        total: 3,
        label: '生成完成',
        active: false
      }
      return created
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '生成资源失败'
      generationProgress.value = null
      return null
    } finally {
      loading.value = false
      setTimeout(() => {
        if (generationProgress.value && !generationProgress.value.active) {
          generationProgress.value = null
        }
      }, 2000)
    }
  }

  function updateProgress(progress: Partial<GenerationProgress>): void {
    if (!generationProgress.value) {
      generationProgress.value = { current: 0, total: 0, label: '', active: true, ...progress }
    } else {
      generationProgress.value = { ...generationProgress.value, ...progress }
    }
  }

  function clearProgress(): void {
    generationProgress.value = null
  }

  function setCurrentResource(resource: LearningResource | null): void {
    currentResource.value = resource
  }

  return {
    resources,
    currentResource,
    generationProgress,
    loading,
    error,
    fetchList,
    fetchDetail,
    generate,
    updateProgress,
    clearProgress,
    setCurrentResource
  }
})

/**
 * 解析资源 content 字段为对应类型。
 */
export function parseResourceContent<T = unknown>(resource: LearningResource): T | null {
  if (!resource.content) return null
  try {
    return JSON.parse(resource.content) as T
  } catch {
    return null
  }
}

export function parseDoc(resource: LearningResource): DocContent | null {
  return parseResourceContent<DocContent>(resource)
}

export function parseMindMap(resource: LearningResource): MindMapContent | null {
  return parseResourceContent<MindMapContent>(resource)
}

export function parseQuiz(resource: LearningResource): QuizContent | null {
  return parseResourceContent<QuizContent>(resource)
}

export function parseReading(resource: LearningResource): ReadingContent | null {
  return parseResourceContent<ReadingContent>(resource)
}

export function parseCode(resource: LearningResource): CodeContent | null {
  return parseResourceContent<CodeContent>(resource)
}
