import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

/**
 * 扩展的请求配置：支持 silent 静默开关。
 *
 * <p>silent=true 时，本次请求失败不弹出全局 ElMessage 错误提示（仍照常 reject），
 * 用于埋点等不应打扰用户的后台请求。
 */
export interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean
}

/**
 * Axios 实例。
 *
 * <p>对应 CLAUDE.md §7.1：
 * <ul>
 *   <li>统一 baseURL（从环境变量读取）</li>
 *   <li>请求拦截：附加 Authorization（如有）</li>
 *   <li>响应拦截：解析后端 Result<T>，code === 200 才放行；其他统一 ElMessage 提示</li>
 * </ul>
 */
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60_000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = window.localStorage.getItem('learngen:token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const payload = response.data as Result<unknown> | undefined
    if (!payload || typeof payload !== 'object' || !('code' in payload)) {
      // 非标准响应：原样返回 axios 响应
      return response
    }
    if (payload.code === 200) {
      return response
    }
    if (!(response.config as RequestConfig)?.silent) {
      ElMessage.error(payload.message || '请求失败')
    }
    return Promise.reject(new Error(payload.message || '请求失败'))
  },
  (error) => {
    const message =
      (error?.response?.data as { message?: string } | undefined)?.message ||
      error?.message ||
      '网络错误'
    if (!(error?.config as RequestConfig | undefined)?.silent) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

/**
 * 统一请求方法：业务 Service 调用后只需关心 data 字段。
 */
async function request<T>(config: RequestConfig): Promise<T> {
  const response = await instance.request<Result<T>>(config)
  const payload = response.data as Result<T> | undefined
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data
  }
  return response.data as unknown as T
}

export default instance
export { request }
