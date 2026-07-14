import { request } from './request'
import type {
  LearningResource,
  GenerateResourceRequest,
  ResourceType
} from '@/types/resource'

/**
 * 查询资源列表。
 */
export function listResources(studentId: number, type?: ResourceType): Promise<LearningResource[]> {
  return request<LearningResource[]>({
    url: '/resources',
    method: 'GET',
    params: { studentId, type }
  })
}

/**
 * 查询资源详情。
 */
export function getResourceById(id: number): Promise<LearningResource> {
  return request<LearningResource>({
    url: `/resources/${id}`,
    method: 'GET'
  })
}

/**
 * 触发生成资源。
 */
export function generateResource(payload: GenerateResourceRequest): Promise<LearningResource> {
  return request<LearningResource>({
    url: '/resources/generate',
    method: 'POST',
    data: payload
  })
}
