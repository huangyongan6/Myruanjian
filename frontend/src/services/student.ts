import { request } from './request'
import type { Student, CreateStudentRequest } from '@/types/student'

/**
 * 创建学生。
 */
export function createStudent(payload: CreateStudentRequest): Promise<Student> {
  return request<Student>({
    url: '/students',
    method: 'POST',
    data: payload
  })
}

/**
 * 查询学生详情。
 */
export function getStudentById(id: number): Promise<Student> {
  return request<Student>({
    url: `/students/${id}`,
    method: 'GET'
  })
}
