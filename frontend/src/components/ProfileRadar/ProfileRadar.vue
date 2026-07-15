<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { StudentProfile, ParsedProfile } from '@/types/profile'
import { safeJsonParse } from '@/utils/format'

interface Props {
  profile: StudentProfile | null
}
const props = defineProps<Props>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const indicators = [
  { name: '知识基础', max: 100 },
  { name: '认知风格', max: 100 },
  { name: '学习目标', max: 100 },
  { name: '易错点', max: 100 },
  { name: '学习节奏', max: 100 },
  { name: '兴趣方向', max: 100 }
]

/**
 * 各维度评分计算（统一归一化到 0-100）：
 *
 * - knowledgeBase: math/programming/ml_familiarity (各 1-5) → 先平均再 *20
 * - cognitiveStyle: visual/textual/hands_on (各 0.0-1.0) → 直接 *100
 * - learningGoal: goal_type + target_direction 有值各得 50 分
 * - weakPoints: weak_topics + mistake_types 数组长度综合评估（越多说明分析越细）
 * - learningPace: daily_hours (小时数 *25，上限100) + pace 转换（slow=33/medium=67/fast=100）
 * - interestArea: areas 数组长度 *20（最多5个满100）+ preferred_project_type 有值 +20
 */
function calcKnowledgeBaseScore(parsed: ParsedProfile): number {
  const kb = parsed.knowledgeBase
  const values: number[] = []
  if (kb.math_level) values.push(kb.math_level)
  if (kb.programming_level) values.push(kb.programming_level)
  if (kb.ml_familiarity) values.push(kb.ml_familiarity)
  if (values.length === 0) return 0
  const avg = values.reduce((a, b) => a + b, 0) / values.length
  return Math.round(avg * 20) // 1-5 → 20-100
}

function calcCognitiveStyleScore(parsed: ParsedProfile): number {
  const cs = parsed.cognitiveStyle
  const values: number[] = []
  if (cs.visual !== undefined) values.push(cs.visual)
  if (cs.textual !== undefined) values.push(cs.textual)
  if (cs.hands_on !== undefined) values.push(cs.hands_on)
  if (values.length === 0) return 0
  const avg = values.reduce((a, b) => a + b, 0) / values.length
  return Math.round(avg * 100) // 0.0-1.0 → 0-100
}

function calcLearningGoalScore(parsed: ParsedProfile): number {
  const lg = parsed.learningGoal
  let score = 0
  if (lg.goal_type && lg.goal_type.trim()) score += 50
  if (lg.target_direction && lg.target_direction.trim()) score += 50
  return score
}

function calcWeakPointsScore(parsed: ParsedProfile): number {
  const wp = parsed.weakPoints
  let score = 0
  if (wp.weak_topics && wp.weak_topics.length > 0) {
    score += Math.min(wp.weak_topics.length * 15, 60) // 最多4个满60
  }
  if (wp.mistake_types && wp.mistake_types.length > 0) {
    score += Math.min(wp.mistake_types.length * 10, 40) // 最多4个满40
  }
  return Math.min(score, 100)
}

function calcLearningPaceScore(parsed: ParsedProfile): number {
  const lp = parsed.learningPace
  let score = 0
  // daily_hours: 0-4小时映射到0-100
  if (lp.daily_hours !== undefined && lp.daily_hours > 0) {
    score += Math.min(lp.daily_hours * 25, 100) * 0.6 // 权重60%
  }
  // pace转换
  if (lp.pace) {
    const paceScore = lp.pace === 'fast' ? 100 : lp.pace === 'medium' ? 67 : lp.pace === 'slow' ? 33 : 0
    score += paceScore * 0.4 // 权重40%
  }
  return Math.min(Math.round(score), 100)
}

function calcInterestAreaScore(parsed: ParsedProfile): number {
  const ia = parsed.interestArea
  let score = 0
  if (ia.areas && ia.areas.length > 0) {
    score += Math.min(ia.areas.length * 20, 80) // 最多4个满80
  }
  if (ia.preferred_project_type && ia.preferred_project_type.trim()) {
    score += 20
  }
  return Math.min(score, 100)
}

function buildOption(profile: StudentProfile | null): echarts.EChartsOption {
  const parsed: ParsedProfile | null = profile
    ? {
        studentId: profile.studentId,
        knowledgeBase: safeJsonParse(profile.knowledgeBase, {}) as ParsedProfile['knowledgeBase'],
        cognitiveStyle: safeJsonParse(profile.cognitiveStyle, {}) as ParsedProfile['cognitiveStyle'],
        learningGoal: safeJsonParse(profile.learningGoal, {}) as ParsedProfile['learningGoal'],
        weakPoints: safeJsonParse(profile.weakPoints, {}) as ParsedProfile['weakPoints'],
        learningPace: safeJsonParse(profile.learningPace, {}) as ParsedProfile['learningPace'],
        interestArea: safeJsonParse(profile.interestArea, {}) as ParsedProfile['interestArea'],
        updatedAt: profile.updatedAt
      }
    : null

  if (!parsed) {
    return {
      radar: { indicator: indicators, radius: '65%' },
      series: [{ type: 'radar', data: [] }]
    }
  }

  const values = [
    calcKnowledgeBaseScore(parsed),
    calcCognitiveStyleScore(parsed),
    calcLearningGoalScore(parsed),
    calcWeakPointsScore(parsed),
    calcLearningPaceScore(parsed),
    calcInterestAreaScore(parsed)
  ]

  return {
    tooltip: {},
    radar: {
      indicator: indicators,
      radius: '65%',
      splitArea: { areaStyle: { color: ['rgba(64,158,255,0.05)', 'rgba(64,158,255,0.1)'] } }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '学习画像',
            areaStyle: { color: 'rgba(64,158,255,0.3)' },
            lineStyle: { color: '#409eff' },
            itemStyle: { color: '#409eff' }
          }
        ]
      }
    ]
  }
}

function render(): void {
  if (!chartRef.value) return
  chart = chart ?? echarts.init(chartRef.value)
  chart.setOption(buildOption(props.profile), true)
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.profile, render, { deep: true })

function resize(): void {
  chart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="profile-radar">
    <div ref="chartRef" class="profile-radar__chart" />
  </div>
</template>

<style scoped lang="scss">
.profile-radar {
  width: 100%;
  &__chart {
    width: 100%;
    height: 400px;
  }
}
</style>
