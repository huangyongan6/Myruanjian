import { request } from './request'
import type { ChatMessage, ChatRequest, ChatResponse } from '@/types/chat'

/**
 * 同步发送对话（REST 入口，便于测试）。
 */
export function sendMessage(payload: ChatRequest): Promise<ChatResponse> {
  return request<ChatResponse>({
    url: '/chat/send',
    method: 'POST',
    data: payload
  })
}

/**
 * 查询对话历史。
 *
 * <p>支持基于游标的向上加载：传入 {@code before}（ISO-8601 时间字符串）时，
 * 仅返回 createdAt 严格早于该时间的消息，按时间倒序。
 */
export function getHistory(studentId: number, limit = 50, before?: string): Promise<ChatMessage[]> {
  return request<ChatMessage[]>({
    url: `/chat/history/${studentId}`,
    method: 'GET',
    params: { limit, before }
  })
}
