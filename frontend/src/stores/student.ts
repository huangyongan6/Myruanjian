import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Student } from '@/types/student'
import { getStudentById, createStudent } from '@/services/student'
import { getItem, setItem, removeItem, STORAGE_KEYS } from '@/utils/storage'

/**
 * 学生 Store。
 *
 * <p>职责：管理当前学生信息、持久化到 localStorage。后端无鉴权，
 * 通过 studentId 区分不同学习者。
 */
export const useStudentStore = defineStore('student', () => {
  const currentStudentId = ref<number | null>(getItem<number>(STORAGE_KEYS.CURRENT_STUDENT_ID))
  const currentStudent = ref<Student | null>(getItem<Student>(STORAGE_KEYS.CURRENT_STUDENT))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isLoggedIn = computed(() => currentStudentId.value !== null)

  async function loadById(id: number): Promise<Student | null> {
    loading.value = true
    error.value = null
    try {
      const student = await getStudentById(id)
      setCurrentStudent(student)
      return student
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载学生失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function createAndSet(name: string): Promise<Student | null> {
    loading.value = true
    error.value = null
    try {
      const student = await createStudent({ name })
      setCurrentStudent(student)
      return student
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '创建学生失败'
      return null
    } finally {
      loading.value = false
    }
  }

  function setCurrentStudent(student: Student): void {
    currentStudent.value = student
    currentStudentId.value = student.id
    setItem(STORAGE_KEYS.CURRENT_STUDENT, student)
    setItem(STORAGE_KEYS.CURRENT_STUDENT_ID, student.id)
  }

  function clearCurrentStudent(): void {
    currentStudent.value = null
    currentStudentId.value = null
    removeItem(STORAGE_KEYS.CURRENT_STUDENT)
    removeItem(STORAGE_KEYS.CURRENT_STUDENT_ID)
  }

  function logout(): void {
    currentStudent.value = null
    currentStudentId.value = null
    removeItem(STORAGE_KEYS.CURRENT_STUDENT)
    removeItem(STORAGE_KEYS.CURRENT_STUDENT_ID)
    removeItem(STORAGE_KEYS.CHAT_HISTORY)
    removeItem(STORAGE_KEYS.LEARNING_PATH)
  }

  return {
    currentStudentId,
    currentStudent,
    loading,
    error,
    isLoggedIn,
    loadById,
    createAndSet,
    setCurrentStudent,
    clearCurrentStudent,
    logout
  }
})
