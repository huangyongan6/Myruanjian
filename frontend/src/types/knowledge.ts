/**
 * 知识点实体（对应后端 KnowledgePoint）。
 */
export interface KnowledgePoint {
  id: number
  module: number
  name: string
  description?: string
  contentPath?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  createdAt?: string
  updatedAt?: string
}

/**
 * 知识点详情响应（point + Markdown 正文）。
 */
export interface KnowledgeDetailResponse {
  point: KnowledgePoint
  markdown: string | null
}
