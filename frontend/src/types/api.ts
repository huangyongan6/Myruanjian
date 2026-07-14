/**
 * 统一 REST 响应体（对应后端 Result<T>）。
 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/**
 * 分页响应体（对应后端 PageResult<T>）。
 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}
