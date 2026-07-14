import { request } from './request'
import type { LearningRecord, RecordRequest, EvaluateReport } from '@/types/record'

/**
 * 记录学习行为。
 */
export function recordAction(payload: RecordRequest): Promise<LearningRecord> {
  return request<LearningRecord>({
    url: '/records',
    method: 'POST',
    data: payload
  })
}

/**
 * 静默埋点：记录学习行为，失败不冒泡、不打扰用户。
 *
 * <p>用于浏览 / 完成 / 答题等学习行为埋点。走 silent 通道抑制全局错误提示，
 * 并吞掉异常，确保埋点失败绝不阻断学习流程。
 */
export async function trackAction(payload: RecordRequest): Promise<void> {
  try {
    await request<LearningRecord>({
      url: '/records',
      method: 'POST',
      data: payload,
      silent: true
    })
  } catch {
    // 埋点失败静默忽略，不影响学习流程
  }
}

/**
 * 查询学生学习记录列表。
 */
export function listRecords(studentId: number): Promise<LearningRecord[]> {
  return request<LearningRecord[]>({
    url: `/records/${studentId}`,
    method: 'GET'
  })
}

/**
 * 学习效果评估报告。
 */
export function evaluateStudent(studentId: number): Promise<EvaluateReport> {
  return request<EvaluateReport>({
    url: `/records/${studentId}/evaluate`,
    method: 'GET'
  })
}
