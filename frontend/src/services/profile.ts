import { request } from './request'
import type { StudentProfile, GenerateProfileRequest } from '@/types/profile'

/**
 * 查询学生画像。
 */
export function getProfile(studentId: number): Promise<StudentProfile> {
  return request<StudentProfile>({
    url: `/profiles/${studentId}`,
    method: 'GET'
  })
}

/**
 * 触发画像抽取（基于提供的对话内容）。
 */
export function generateProfile(studentId: number, payload: GenerateProfileRequest): Promise<StudentProfile> {
  return request<StudentProfile>({
    url: `/profiles/${studentId}/generate`,
    method: 'POST',
    data: payload
  })
}
