import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { StudentProfile, ParsedProfile } from '@/types/profile'
import { getProfile, generateProfile } from '@/services/profile'
import { safeJsonParse } from '@/utils/format'

/**
 * 学生画像 Store。
 */
export const useProfileStore = defineStore('profile', () => {
  const profile = ref<StudentProfile | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const parsed = computed<ParsedProfile | null>(() => {
    if (!profile.value) return null
    return {
      studentId: profile.value.studentId,
      knowledgeBase: safeJsonParse(profile.value.knowledgeBase, {}) as ParsedProfile['knowledgeBase'],
      cognitiveStyle: safeJsonParse(profile.value.cognitiveStyle, {}) as ParsedProfile['cognitiveStyle'],
      learningGoal: safeJsonParse(profile.value.learningGoal, {}) as ParsedProfile['learningGoal'],
      weakPoints: safeJsonParse(profile.value.weakPoints, {}) as ParsedProfile['weakPoints'],
      learningPace: safeJsonParse(profile.value.learningPace, {}) as ParsedProfile['learningPace'],
      interestArea: safeJsonParse(profile.value.interestArea, {}) as ParsedProfile['interestArea'],
      updatedAt: profile.value.updatedAt
    }
  })

  async function fetchProfile(studentId: number): Promise<StudentProfile | null> {
    loading.value = true
    error.value = null
    try {
      profile.value = await getProfile(studentId)
      return profile.value
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载画像失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function generate(studentId: number, content: string): Promise<StudentProfile | null> {
    loading.value = true
    error.value = null
    try {
      profile.value = await generateProfile(studentId, { content })
      return profile.value
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '画像生成失败'
      return null
    } finally {
      loading.value = false
    }
  }

  function clear(): void {
    profile.value = null
    error.value = null
  }

  return { profile, parsed, loading, error, fetchProfile, generate, clear }
})
