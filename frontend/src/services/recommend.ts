import { request } from './request'
import type { RecommendedResource } from '@/types/recommend'

/**
 * 获取学生推荐资源。
 */
export function recommendResources(studentId: number, limit = 5): Promise<RecommendedResource[]> {
  return request<RecommendedResource[]>({
    url: `/recommend/${studentId}`,
    method: 'GET',
    params: { limit }
  })
}
