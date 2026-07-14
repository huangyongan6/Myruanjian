/**
 * 学生实体（对应后端 Student）。
 */
export interface Student {
  id: number
  name: string
  avatar?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建学生请求体。
 */
export interface CreateStudentRequest {
  name: string
  avatar?: string
}
