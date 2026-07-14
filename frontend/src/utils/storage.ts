/**
 * localStorage 键常量（统一管理避免散落）。
 */
export const STORAGE_KEYS = {
  CURRENT_STUDENT_ID: 'learngen:current_student_id',
  CURRENT_STUDENT: 'learngen:current_student',
  CHAT_HISTORY: 'learngen:chat_history',
  PREFERRED_LANGUAGE: 'learngen:preferred_language'
} as const

export type StorageKey = (typeof STORAGE_KEYS)[keyof typeof STORAGE_KEYS]

/**
 * 读取 localStorage（自动 JSON 反序列化）。
 */
export function getItem<T>(key: StorageKey | string, fallback: T | null = null): T | null {
  try {
    const raw = window.localStorage.getItem(key)
    if (raw === null) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

/**
 * 写入 localStorage（自动 JSON 序列化）。
 */
export function setItem<T>(key: StorageKey | string, value: T): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // 静默失败（quota exceeded 等）
  }
}

/**
 * 删除 localStorage 项。
 */
export function removeItem(key: StorageKey | string): void {
  try {
    window.localStorage.removeItem(key)
  } catch {
    // 静默失败
  }
}
