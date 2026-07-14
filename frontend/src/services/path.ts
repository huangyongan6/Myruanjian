import { request } from './request'
import type { LearningPath, GeneratePathRequest } from '@/types/path'

/**
 * 触发学习路径规划。
 */
export function generatePath(payload: GeneratePathRequest): Promise<LearningPath> {
  return request<LearningPath>({
    url: '/paths/generate',
    method: 'POST',
    data: payload
  })
}

/**
 * 查询学生最新学习路径。
 */
export function getLatestPath(studentId: number): Promise<LearningPath> {
  return request<LearningPath>({
    url: `/paths/${studentId}`,
    method: 'GET'
  })
}

/**
 * 更新学习路径进度。
 */
export function updatePathCurrentStep(studentId: number, currentStep: number): Promise<LearningPath> {
  return request<LearningPath>({
    url: `/paths/${studentId}/current-step`,
    method: 'PUT',
    data: { currentStep }
  })
}
