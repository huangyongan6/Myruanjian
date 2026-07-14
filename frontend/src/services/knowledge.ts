import { request } from './request'
import type { KnowledgePoint, KnowledgeDetailResponse } from '@/types/knowledge'

/**
 * 按模块列出知识点。
 */
export function listKnowledgeByModule(module?: number): Promise<KnowledgePoint[]> {
  return request<KnowledgePoint[]>({
    url: '/knowledge',
    method: 'GET',
    params: { module }
  })
}

/**
 * 按名称模糊检索知识点。
 */
export function searchKnowledgeByName(name: string): Promise<KnowledgePoint[]> {
  return request<KnowledgePoint[]>({
    url: '/knowledge/search',
    method: 'GET',
    params: { name }
  })
}

/**
 * 查询知识点详情（含 Markdown 正文）。
 */
export function getKnowledgeDetail(id: number): Promise<KnowledgeDetailResponse> {
  return request<KnowledgeDetailResponse>({
    url: `/knowledge/${id}`,
    method: 'GET'
  })
}
