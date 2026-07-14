import type { LearningResource } from './resource'

/**
 * 推荐结果（资源 + 推荐理由）。
 */
export interface RecommendedResource {
  resource: LearningResource
  reason: string
}
