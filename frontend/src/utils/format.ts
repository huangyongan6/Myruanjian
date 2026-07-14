import type { ResourceType } from '@/types/resource'

/**
 * 格式化日期时间字符串（兼容 ISO 与 'yyyy-MM-dd HH:mm:ss'）。
 */
export function formatDateTime(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

/**
 * 格式化时长（秒 → "X 分 Y 秒"）。
 */
export function formatDuration(seconds?: number): string {
  if (seconds === undefined || seconds === null) return '-'
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  const remain = seconds % 60
  if (minutes < 60) return remain > 0 ? `${minutes} 分 ${remain} 秒` : `${minutes} 分`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours} 小时 ${mins} 分` : `${hours} 小时`
}

/**
 * 资源类型显示名称。
 */
export function getResourceTypeLabel(type?: string): string {
  const map: Record<string, string> = {
    doc: '课程讲解',
    mindmap: '思维导图',
    quiz: '练习题库',
    reading: '拓展阅读',
    code: '代码实操'
  }
  return map[type ?? ''] ?? '未知类型'
}

/**
 * 资源类型 Element Plus Tag 类型。
 */
export function getResourceTypeTagType(
  type?: string
): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    doc: 'primary',
    mindmap: 'info',
    quiz: 'warning',
    reading: 'success',
    code: 'danger'
  }
  return map[type ?? ''] ?? 'info'
}

/**
 * 资源类型对应图标组件名。
 */
export function getResourceTypeIcon(type?: string): string {
  const map: Record<string, string> = {
    doc: 'Document',
    quiz: 'EditPen',
    reading: 'Reading',
    code: 'Cpu'
  }
  return map[type ?? ''] ?? 'Files'
}

/**
 * 难度显示。
 */
export function getDifficultyLabel(difficulty?: string): string {
  const map: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return map[difficulty ?? ''] ?? '中等'
}

/**
 * 难度 Tag 类型。
 */
export function getDifficultyTagType(difficulty?: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger'
  }
  return map[difficulty ?? ''] ?? 'info'
}

/**
 * 安全 JSON 解析（容错）。
 */
export function safeJsonParse<T = unknown>(value?: string | null, fallback: T | null = null): T | null {
  if (!value) return fallback
  try {
    return JSON.parse(value) as T
  } catch {
    return fallback
  }
}

/**
 * 获取所有资源类型列表（供筛选下拉用）。
 */
export function getAllResourceTypes(): ResourceType[] {
  return ['doc', 'mindmap', 'quiz', 'reading', 'code']
}
