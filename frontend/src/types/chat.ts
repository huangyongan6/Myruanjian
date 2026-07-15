/**
 * 对话角色。
 */
export type ChatRole = 'user' | 'assistant' | 'system'

/**
 * 对话消息实体（对应后端 ChatMessage）。
 */
export interface ChatMessage {
  id?: number
  studentId: number
  role: ChatRole
  content: string
  agentType?: string
  /** 逻辑删除标记，true = 已删除（被打断的对话） */
  deleted?: boolean
  createdAt?: string
}

/**
 * 对话发送请求体（REST + WebSocket 共用）。
 */
export interface ChatRequest {
  studentId: number
  content: string
}

/**
 * 对话响应（REST 与 WebSocket 推送统一结构）。
 */
export interface ChatResponse {
  type: 'message' | 'progress' | 'resource' | 'error' | 'done'
  content: string
  timestamp: string
  agentType?: string
  payload?: unknown
}
