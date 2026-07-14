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
  { name: '数学基础', max: 100 },
  { name: '编程能力', max: 100 },
  { name: 'ML 熟悉度', max: 100 },
  { name: '学习目标', max: 100 },
  { name: '学习节奏', max: 100 },
  { name: '认知风格', max: 100 }
]

function calcScore(parsed: ParsedProfile | null, key: keyof ParsedProfile): number {
  if (!parsed) return 0
  const data = parsed[key] as Record<string, number | undefined>
  const values = Object.values(data ?? {}).filter((v): v is number => typeof v === 'number')
  if (values.length === 0) return 0
  const sum = values.reduce((acc, v) => acc + v, 0)
  return Math.round((sum / values.length) * 100)
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

  const values = [
    calcScore(parsed, 'knowledgeBase'),
    calcScore(parsed, 'cognitiveStyle'),
    calcScore(parsed, 'learningGoal'),
    calcScore(parsed, 'learningPace'),
    calcScore(parsed, 'interestArea'),
    parsed ? Math.max(calcScore(parsed, 'cognitiveStyle'), 50) : 0
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
  chart.setOption(buildOption(props.profile))
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
