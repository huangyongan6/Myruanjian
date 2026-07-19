<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import ProfileRadar from '@/components/ProfileRadar/ProfileRadar.vue'
import { useProfileStore } from '@/stores/profile'
import { useStudentStore } from '@/stores/student'
import { useChatStore } from '@/stores/chat'
import { usePathStore } from '@/stores/path'
import { formatDateTime } from '@/utils/format'
import type { KnowledgeBase, CognitiveStyle, LearningGoal, WeakPoints, LearningPace, InterestArea } from '@/types/profile'

const profileStore = useProfileStore()
const studentStore = useStudentStore()
const chatStore = useChatStore()
const pathStore = usePathStore()

const studentId = computed(() => studentStore.currentStudentId)

/**
 * 知识基础：将数值映射为中文档位（与 PromptTemplates 中 1-5 量纲一致）。
 * 1=薄弱，2=了解/基础，3=入门/中等，4=熟练/较好，5=精通/扎实。
 */
function mathLevelLabel(value?: number): string {
  if (value === undefined || value === null) return '未知'
  if (value >= 5) return '精通（竞赛水平）'
  if (value >= 4) return '较好（高中水平）'
  if (value >= 3) return '入门（初中水平）'
  if (value >= 2) return '了解（小学水平）'
  if (value >= 1) return '薄弱'
  return '未知'
}
function programmingLevelLabel(value?: number): string {
  if (value === undefined || value === null) return '未知'
  if (value >= 5) return '精通（可架构设计）'
  if (value >= 4) return '熟练（可独立完成项目）'
  if (value >= 3) return '入门（掌握基础语法）'
  if (value >= 2) return '了解（写过简单代码）'
  if (value >= 1) return '零基础'
  return '未知'
}
function mlFamiliarityLabel(value?: number): string {
  if (value === undefined || value === null) return '未知'
  if (value >= 5) return '精通（掌握原理与源码）'
  if (value >= 4) return '熟悉（了解常见模型原理）'
  if (value >= 3) return '入门（知道概念与基本流程）'
  if (value >= 2) return '了解（听过专有名词）'
  if (value >= 1) return '零基础'
  return '未知'
}

/**
 * 认知风格：把 0.0-1.0 的数值转换成倾向描述。
 * 阈值：>=0.7 强偏好，>=0.5 较偏好，>=0.3 一般，>=0.15 较弱。
 */
function tendencyLabel(value?: number): string {
  if (value === undefined || value === null) return '未知'
  if (value >= 0.7) return '强偏好'
  if (value >= 0.5) return '较偏好'
  if (value >= 0.3) return '一般'
  if (value >= 0.15) return '较弱'
  return '低'
}

/**
 * 学习节奏文案。
 */
function paceLabel(pace?: string): string {
  const map: Record<string, string> = {
    fast: '快节奏（短时间内大量输入）',
    medium: '中等节奏（循序渐进）',
    slow: '慢节奏（充分消化后再推进）'
  }
  return map[pace ?? ''] ?? pace ?? '未提供'
}

/**
 * 每日学习时长格式化：数字小时 -> "X小时XX分钟"。
 * 例如 1.5 -> "1小时30分钟"，0.5 -> "30分钟"。
 */
function formatDailyHours(hours?: number): string {
  if (hours === undefined || hours === null) return '未提供'
  if (hours <= 0) return '0分钟'
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (h > 0 && m > 0) return `${h}小时${m}分钟`
  if (h > 0) return `${h}小时`
  return `${m}分钟`
}

/**
 * 数组字段安全访问，缺失或非数组时返回空数组。
 */
function safeArray<T>(value: T[] | undefined | null): T[] {
  return Array.isArray(value) ? value : []
}

const knowledgeBase = computed<KnowledgeBase>(() => profileStore.parsed?.knowledgeBase ?? {})
const cognitiveStyle = computed<CognitiveStyle>(() => profileStore.parsed?.cognitiveStyle ?? {})
const learningGoal = computed<LearningGoal>(() => profileStore.parsed?.learningGoal ?? {})
const weakPoints = computed<WeakPoints>(() => profileStore.parsed?.weakPoints ?? {})
const learningPace = computed<LearningPace>(() => profileStore.parsed?.learningPace ?? {})
const interestArea = computed<InterestArea>(() => profileStore.parsed?.interestArea ?? {})

async function loadProfile(): Promise<void> {
  if (studentId.value === null) {
    ElMessage.warning('请先在对话页选择学生')
    return
  }
  await Promise.all([
    profileStore.fetchProfile(studentId.value),
    pathStore.fetchLatest(studentId.value)
  ])
  // 如果画像不存在，基于学习数据自动生成
  if (!profileStore.profile) {
    await autoGenerateProfile()
  }
}

async function autoGenerateProfile(): Promise<void> {
  if (studentId.value === null) return
  // 获取对话历史（可选）
  const list = chatStore.getMessages(studentId.value)
  const content = list.length > 0
    ? list.slice(-20).map((m) => `${m.role === 'user' ? '学生' : 'AI'}：${m.content}`).join('\n')
    : ''
  try {
    await profileStore.generate(studentId.value, content)
  } catch {
    // 生成失败不阻塞页面加载
  }
}

async function regenerate(): Promise<void> {
  if (studentId.value === null) return
  const list = chatStore.getMessages(studentId.value)
  const content = list.length > 0
    ? list.slice(-20).map((m) => `${m.role === 'user' ? '学生' : 'AI'}：${m.content}`).join('\n')
    : ''
  await profileStore.generate(studentId.value, content)
  ElMessage.success('画像已更新')
}

onMounted(loadProfile)
</script>

<template>
  <div class="page-container profile-page">
    <div class="profile-page__actions">
      <el-button :loading="profileStore.loading" type="primary" @click="regenerate">
        <IconSvg name="refresh" :size="16" /> 基于对话重新生成画像
      </el-button>
    </div>

    <div v-if="profileStore.loading && !profileStore.profile" class="empty-tip">加载中...</div>
    <el-empty v-else-if="!profileStore.profile" description="暂无画像，点击下方按钮生成" />

    <template v-else>
      <el-card class="profile-page__radar-card" shadow="never">
        <template #header>
          <div class="profile-page__card-header">
            <span><IconSvg name="chart" :size="16" /> 6 维能力雷达</span>
            <span class="profile-page__updated">更新于 {{ formatDateTime(profileStore.profile?.updatedAt) }}</span>
          </div>
        </template>
        <ProfileRadar :profile="profileStore.profile" />
      </el-card>

      <!-- 学习路线总览 -->
      <el-card v-if="pathStore.currentPath" class="profile-page__path-card" shadow="never">
        <template #header>
          <div class="profile-page__card-header">
            <span><IconSvg name="compass" :size="16" /> 学习路线进度</span>
            <el-tag :type="pathStore.progressPercent >= 100 ? 'success' : 'primary'" size="small">
              {{ pathStore.progressPercent }}%
            </el-tag>
          </div>
        </template>
        <div class="profile-page__path-info">
          <div class="profile-page__path-item">
            <span class="profile-page__path-label">当前进度</span>
            <span class="profile-page__path-value">
              第 {{ pathStore.currentStepIndex }} / {{ pathStore.currentPath.totalSteps }} 步
            </span>
          </div>
          <el-progress
            :percentage="pathStore.progressPercent"
            :status="pathStore.progressPercent >= 100 ? 'success' : ''"
            :stroke-width="10"
            class="profile-page__progress"
          />
        </div>
      </el-card>

      <el-row :gutter="16" class="profile-page__details">
        <!-- 📚 知识基础 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header><span><IconSvg name="book" :size="16" /> 知识基础</span></template>
            <ul class="profile-page__list">
              <li class="profile-page__item">
                <span class="profile-page__label">数学水平</span>
                <span class="profile-page__value">{{ mathLevelLabel(knowledgeBase.math_level) }}</span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">编程能力</span>
                <span class="profile-page__value">{{ programmingLevelLabel(knowledgeBase.programming_level) }}</span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">机器学习熟悉度</span>
                <span class="profile-page__value">{{ mlFamiliarityLabel(knowledgeBase.ml_familiarity) }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>

        <!-- 🧠 认知风格 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header><span><IconSvg name="brain" :size="16" /> 认知风格</span></template>
            <ul class="profile-page__list">
              <li class="profile-page__item">
                <span class="profile-page__label">视觉型</span>
                <span class="profile-page__value">{{ tendencyLabel(cognitiveStyle.visual) }}</span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">文字型</span>
                <span class="profile-page__value">{{ tendencyLabel(cognitiveStyle.textual) }}</span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">动手实操型</span>
                <span class="profile-page__value">{{ tendencyLabel(cognitiveStyle.hands_on) }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>

        <!-- 🎯 学习目标 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="profile-page__card--equal-height">
            <template #header><span><IconSvg name="target" :size="16" /> 学习目标</span></template>
            <ul class="profile-page__list">
              <li class="profile-page__item">
                <span class="profile-page__label">目标类型</span>
                <span class="profile-page__value">{{ learningGoal.goal_type || '未提供' }}</span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">目标方向</span>
                <span class="profile-page__value">{{ learningGoal.target_direction || '未提供' }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>

        <!-- ⚠️ 易错点 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="profile-page__card--equal-height">
            <template #header><span><IconSvg name="warning" :size="16" /> 易错点</span></template>
            <div class="profile-page__section">
              <div class="profile-page__sub-label">薄弱知识点</div>
              <div v-if="safeArray(weakPoints.weak_topics).length === 0" class="profile-page__empty">暂无</div>
              <ul v-else class="profile-page__tags">
                <li v-for="topic in safeArray(weakPoints.weak_topics)" :key="topic" class="profile-page__tag profile-page__tag--warn">
                  {{ topic }}
                </li>
              </ul>
            </div>
            <div class="profile-page__section">
              <div class="profile-page__sub-label">常见错误类型</div>
              <div v-if="safeArray(weakPoints.mistake_types).length === 0" class="profile-page__empty">暂无</div>
              <ul v-else class="profile-page__tags">
                <li v-for="type in safeArray(weakPoints.mistake_types)" :key="type" class="profile-page__tag">
                  {{ type }}
                </li>
              </ul>
            </div>
          </el-card>
        </el-col>

        <!-- ⏱ 学习节奏 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="profile-page__card--equal-height">
            <template #header><span><IconSvg name="clock" :size="16" /> 学习节奏</span></template>
            <ul class="profile-page__list">
              <li class="profile-page__item">
                <span class="profile-page__label">每日学习时长</span>
                <span class="profile-page__value">
                  {{ formatDailyHours(learningPace.daily_hours) }}
                </span>
              </li>
              <li class="profile-page__item">
                <span class="profile-page__label">节奏偏好</span>
                <span class="profile-page__value">{{ paceLabel(learningPace.pace) }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>

        <!-- 💡 兴趣方向 -->
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="profile-page__card--equal-height">
            <template #header><span><IconSvg name="idea" :size="16" /> 兴趣方向</span></template>
            <div class="profile-page__section">
              <div class="profile-page__sub-label">兴趣领域</div>
              <div v-if="safeArray(interestArea.areas).length === 0" class="profile-page__empty">暂无</div>
              <ul v-else class="profile-page__tags">
                <li v-for="area in safeArray(interestArea.areas)" :key="area" class="profile-page__tag profile-page__tag--primary">
                  {{ area }}
                </li>
              </ul>
            </div>
            <div class="profile-page__section">
              <div class="profile-page__sub-label">偏好项目类型</div>
              <div class="profile-page__value profile-page__value--block">
                {{ interestArea.preferred_project_type || '未提供' }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  &__actions {
    margin-bottom: $spacing-md;
    display: flex;
    gap: $spacing-sm;
  }
  &__radar-card {
    margin-bottom: $spacing-md;
    border-radius: $radius-lg;
    border: 1px solid $border-light;
    transition: all $transition-normal;

    &:hover {
      box-shadow: $shadow-hover;
    }

    :deep(.el-card__header) {
      border-bottom: 1px solid $border-light;
      padding: $spacing-md $spacing-lg;
    }

    :deep(.el-card__body) {
      padding: $spacing-lg;
    }
  }
  &__path-card {
    margin-bottom: $spacing-md;
    border-radius: $radius-lg;
    border: 1px solid $border-light;
    transition: all $transition-normal;

    &:hover {
      box-shadow: $shadow-hover;
    }

    :deep(.el-card__header) {
      border-bottom: 1px solid $border-light;
      padding: $spacing-md $spacing-lg;
    }

    :deep(.el-card__body) {
      padding: $spacing-lg;
    }
  }
  &__path-info {
    padding: $spacing-xs 0;
  }
  &__path-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
  }
  &__path-label {
    color: $text-secondary;
    font-size: 13px;
    font-weight: 500;
  }
  &__path-value {
    color: $text-primary;
    font-size: 15px;
    font-weight: 600;
  }
  &__progress {
    margin-top: $spacing-sm;

    :deep(.el-progress__bar) {
      border-radius: $radius-full;
    }
  }
  &__card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    color: $text-primary;
  }
  &__updated {
    font-size: 12px;
    color: $text-secondary;
    font-weight: 400;
  }
  &__details {
    margin-top: 0;
  }

  &__card--equal-height {
    height: 100%;
    border-radius: $radius-lg;
    border: 1px solid $border-light;
    transition: all $transition-normal;

    &:hover {
      box-shadow: $shadow-hover;
    }

    :deep(.el-card__body) {
      min-height: 100px;
      padding: $spacing-lg;
    }

    :deep(.el-card__header) {
      border-bottom: 1px solid $border-light;
      padding: $spacing-md $spacing-lg;
    }
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }
  &__item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md 0;
    border-bottom: 1px solid $border-lighter;

    &:last-child {
      border-bottom: none;
    }
  }
  &__label {
    color: $text-secondary;
    font-size: 13px;
    flex-shrink: 0;
    margin-right: $spacing-md;
    font-weight: 500;
  }
  &__value {
    color: $text-primary;
    font-size: 14px;
    font-weight: 500;
    text-align: right;
    word-break: break-word;
  }
  &__value--block {
    display: block;
    text-align: left;
    margin-top: $spacing-xs;
  }

  &__section + &__section {
    margin-top: $spacing-lg;
    padding-top: $spacing-lg;
    border-top: 1px solid $border-lighter;
  }
  &__sub-label {
    color: $text-secondary;
    font-size: 12px;
    margin-bottom: $spacing-sm;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  &__empty {
    color: $text-placeholder;
    font-size: 13px;
    padding: $spacing-xs 0;
  }

  &__tags {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
  }
  &__tag {
    display: inline-flex;
    align-items: center;
    padding: 6px 12px;
    border-radius: $radius-full;
    background: $border-lighter;
    color: $text-regular;
    font-size: 13px;
    font-weight: 500;
    line-height: 1.4;
    transition: all $transition-fast;

    &:hover {
      transform: translateY(-1px);
    }

    &--warn {
      background: rgba(239, 68, 68, 0.08);
      color: $danger-color;
    }
    &--primary {
      background: rgba(59, 130, 246, 0.08);
      color: $primary-color;
    }
  }
}
</style>