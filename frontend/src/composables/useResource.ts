import { computed } from 'vue'
import { useResourceStore } from '@/stores/resource'
import type { ResourceType } from '@/types/resource'

/**
 * 资源加载 Composable。
 *
 * <p>封装列表加载、详情加载、生成等动作；自动处理 5 种类型的 content 解析。
 */
export function useResource() {
  const store = useResourceStore()

  async function loadList(studentId: number, type?: ResourceType): Promise<void> {
    await store.fetchList(studentId, type)
  }

  async function loadDetail(id: number): Promise<void> {
    await store.fetchDetail(id)
  }

  async function generateResource(studentId: number, type: ResourceType, knowledgePoint: string): Promise<void> {
    store.updateProgress({ current: 1, total: 3, label: '正在调起 Agent...', active: true })
    await store.generate({ studentId, type, knowledgePoint })
  }

  return {
    resources: computed(() => store.resources),
    currentResource: computed(() => store.currentResource),
    generationProgress: computed(() => store.generationProgress),
    loading: computed(() => store.loading),
    error: computed(() => store.error),
    loadList,
    loadDetail,
    generateResource,
    clearProgress: store.clearProgress
  }
}
