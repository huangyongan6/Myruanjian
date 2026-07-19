<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import { useStudentStore } from '@/stores/student'
import { listRecords, evaluateStudent } from '@/services/record'
import { useWebSocket } from '@/composables/useWebSocket'
import { formatDuration } from '@/utils/format'
import type { LearningRecord, EvaluateReport } from '@/types/record'

const studentStore = useStudentStore()
const records = ref<LearningRecord[]>([])
const report = ref<EvaluateReport | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)
const barChartRef = ref<HTMLDivElement | null>(null)
let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

// 实时跟新相关
const lastUpdatedAt = ref<Date | null>(null)
const refreshing = ref(false)
/** 当前订阅的目的地，null 表示未订阅；用于切换学生时主动退订旧订阅 */
const subscribedDestination = ref<string | null>(null)
let refreshTimer: number | null = null

/**
 * STOMP 复用项目已有 useWebSocket（STOMP over SockJS）。Dashboard 拥有
 * 独立的订阅生命周期：进入页面订阅当前学生，离页面或切换学生时先取消
 * 旧订阅，避免与对话页订阅串扰或重复触发回调。
 */
const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || '/ws'
const ws = useWebSocket(wsBaseUrl)

const studentId = computed(() => studentStore.currentStudentId)

/**
 * 行为类型常量：与后端 LearningRecord.action 取值保持一致。
 * 用常量避免硬编码字符串散落各处，便于后续扩展时统一定义。
 */
const ACTION = {
  VIEW: 'view',
  COMPLETE: 'complete',
  QUIZ: 'quiz'
} as const

/**
 * 一次真实学习投入产生的"有效时长"：完成资源 + 答题 + 浏览（有实际时长）。
 *
 * <p>说明：任何学习行为（view / complete / quiz）只要有 duration 就计入累计时长。
 * view 行为在关闭详情弹窗时会传入实际浏览时长；complete 和 quiz 行为如果传入了
 * duration 也会被计入。该口径与"亮点 / 建议"中的"学习时长"语义保持一致。
 */
function isEffectiveLearningRecord(r: LearningRecord): boolean {
  return (r.duration ?? 0) > 0
}

/**
 * 累计学习时长（秒）：统计所有有实际时长（duration > 0）的记录之和。
 *
 * <p>说明：前端派生时只保留有实际时长的记录（duration > 0），这样无论是
 * 浏览资源（关闭弹窗时结算）、完成资源还是答题，只要有实际时长都会被计入。
 * 后端 report 字段仅作缺失兜底。
 */
const totalDuration = computed(() => {
  return records.value
    .filter(isEffectiveLearningRecord)
    .reduce((acc, r) => acc + (r.duration ?? 0), 0)
})

/** 浏览资源数（去重）：以 evaluate 报告为权威，缺失时按前端 records 去重兜底 */
const totalViewed = computed(() => {
  const fromReport = report.value?.viewCount
  if (typeof fromReport === 'number') return fromReport
  // 前端兜底：按 resourceId 去重，与后端 evaluate 语义保持一致
  const viewed = new Set<number>()
  records.value
    .filter((r) => r.action === ACTION.VIEW && r.resourceId != null)
    .forEach((r) => viewed.add(r.resourceId as number))
  return viewed.size
})

/** 完成资源次数 */
const totalCompleted = computed(() => {
  const fromReport = report.value?.completeCount
  if (typeof fromReport === 'number') return fromReport
  return records.value.filter((r) => r.action === ACTION.COMPLETE).length
})

/** 答题次数 */
const totalQuiz = computed(() => {
  const fromReport = report.value?.quizCount
  if (typeof fromReport === 'number') return fromReport
  return records.value.filter((r) => r.action === ACTION.QUIZ).length
})

/**
 * 答题平均分：仅对 quiz 行为且带分数的记录求平均，保留 1 位小数。
 *
 * <p>修复前版本用 {@code r.score} 是否为数字作为筛选条件，会把 view/complete
 * 误带 score 的脏数据也纳入；且无答题时返回 0，与报告区"答题平均分"使用
 * {@code null} 兜底的口径不一致。这里统一返回 {@code number | null}，
 * 让模板用 {@code -} 兜底显示，避免 0 分被误判为"无数据"。
 */
const averageScore = computed<number | null>(() => {
  const fromReport = report.value?.averageScore
  if (typeof fromReport === 'number') return fromReport
  const quizScores = records.value
    .filter((r) => r.action === ACTION.QUIZ && typeof r.score === 'number')
    .map((r) => r.score as number)
  if (quizScores.length === 0) return null
  const avg = quizScores.reduce((a, b) => a + b, 0) / quizScores.length
  return Math.round(avg * 10) / 10
})

/**
 * 报告中的指标，源自后端 LearningRecordServiceImpl#evaluate。
 *
 * <p>统一以顶部 4 个 {@code totalXxx} computed 作为权威源（它们自身已经
 * 优先取自 evaluate 报告，缺失时按前端明细派生），保证顶部卡片、报告区
 * stat tile、柱状图三个数据出口数字完全一致。
 */
const reportViewCount = computed(() => totalViewed.value)
const reportCompleteCount = computed(() => totalCompleted.value)
const reportQuizCount = computed(() => totalQuiz.value)
const reportTotalRecords = computed(() => {
  const raw = report.value?.totalRecords
  return typeof raw === 'number' ? raw : records.value.length
})
const reportCompletionRate = computed(() => {
  if (typeof report.value?.completionRate === 'number') return report.value.completionRate
  const denom = reportViewCount.value + reportCompleteCount.value
  return denom === 0 ? 0 : reportCompleteCount.value / denom
})
/** 答题平均分：null 表示无答题数据 */
const reportAverageScore = computed<number | null>(() => averageScore.value)
/** 累计学习时长（秒）：与顶部"累计学习时长"完全一致 */
const reportTotalDurationSeconds = computed(() => totalDuration.value)

/** 学习时长：分钟（保留 1 位小数） */
const reportTotalDurationMinutes = computed(() => {
  const seconds = reportTotalDurationSeconds.value
  return Math.round((seconds / 60) * 10) / 10
})

/**
 * 答题平均分格式化：null → '-'，有效分数保留 1 位小数。
 * 抽成函数避免模板里出现重复三元，且杜绝 {@code score || '-'} 把 0 误判为无值。
 */
function formatScore(score: number | null | undefined): string {
  if (score === null || score === undefined || Number.isNaN(score)) return '-'
  return score.toFixed(1)
}

/** 后端当前不直接生成弱项主题，预留类型字段后由前端规则兜底生成 */
const reportWeakTopics = computed<string[]>(() => {
  const raw = report.value?.weakTopics
  if (Array.isArray(raw) && raw.length > 0) return raw
  return []
})

/**
 * 基于当前数据生成"评估报告"文字说明。
 * 输出三段：总览、亮点与不足、改进建议。
 */
const reportOverview = computed(() => {
  const total = reportTotalRecords.value
  if (total === 0) {
    return '暂无学习记录。学生还未开始任何学习行为，建议先安排一个引导性资源完成首次浏览。'
  }
  const minutes = reportTotalDurationMinutes.value
  const rate = (reportCompletionRate.value * 100).toFixed(1)
  return `该学生累计产生 ${total} 条学习记录，累计学习时长约 ${minutes} 分钟；其中浏览 ${reportViewCount.value} 次、完成 ${reportCompleteCount.value} 个资源、参与答题 ${reportQuizCount.value} 次，整体完成率 ${rate}%。`
})

const reportHighlights = computed<string[]>(() => {
  const items: string[] = []
  const minutes = reportTotalDurationMinutes.value
  if (minutes >= 30) items.push(`累计学习时长达到 ${minutes} 分钟，学习投入较为稳定。`)
  const rate = reportCompletionRate.value
  if (rate >= 0.6) items.push(`资源完成率达到 ${(rate * 100).toFixed(1)}%，对推荐内容的吸收效率较高。`)
  const avg = reportAverageScore.value
  if (avg !== null && avg >= 80) items.push(`答题平均分 ${avg.toFixed(1)}，知识掌握情况良好。`)
  if (reportQuizCount.value >= 5) items.push(`已累计完成 ${reportQuizCount.value} 次答题，互动反馈数据充足。`)
  if (items.length === 0) items.push('当前尚无可量化的明显亮点，建议持续记录学习行为后再观察。')
  return items
})

const reportConcerns = computed<string[]>(() => {
  const items: string[] = []
  const total = reportTotalRecords.value
  const rate = reportCompletionRate.value
  const avg = reportAverageScore.value
  if (total > 0 && rate < 0.4) items.push(`完成率仅 ${(rate * 100).toFixed(1)}%，存在大量"浏览未完成"的情况。`)
  if (reportQuizCount.value === 0) items.push('尚未参与任何答题，缺乏对掌握程度的客观反馈。')
  if (avg !== null && avg < 60) items.push(`答题平均分偏低（${avg.toFixed(1)}），存在明显的薄弱环节。`)
  if (reportTotalDurationSeconds.value > 0 && reportTotalDurationMinutes.value < 5) items.push('单次学习时长偏短，建议安排更完整的连续学习时段。')
  if (items.length === 0) items.push('未发现明显短板，可继续保持当前节奏。')
  return items
})

const reportSuggestions = computed<string[]>(() => {
  const items: string[] = []
  const rate = reportCompletionRate.value
  const avg = reportAverageScore.value
  const quizCount = reportQuizCount.value
  const minutes = reportTotalDurationMinutes.value

  if (reportWeakTopics.value.length > 0) {
    items.push(`针对薄弱主题「${reportWeakTopics.value.join('、')}」补充配套练习与导图。`)
  }
  if (rate < 0.5 && reportTotalRecords.value > 0) {
    items.push('降低单次推送资源粒度，改为"小步快跑"，提升完成率。')
  }
  if (quizCount < 3) {
    items.push('在后续路径节点中插入更多诊断题，以补充答题数据。')
  }
  if (avg !== null && avg < 70) {
    items.push('在讲解类资源之外增加例题与错题回顾，强化巩固。')
  }
  if (minutes < 15 && reportTotalRecords.value > 0) {
    items.push('建议每日固定 15-30 分钟的连续学习时段，养成稳定节奏。')
  }
  if (items.length === 0) {
    items.push('维持当前学习节奏，并适时挑战难度更高的资源以继续提升。')
  }
  return items
})

async function loadAll(): Promise<void> {
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  refreshing.value = true
  try {
    records.value = await listRecords(studentId.value)
  } catch {
    records.value = []
  }
  try {
    report.value = await evaluateStudent(studentId.value)
  } catch {
    report.value = null
  }
  renderCharts()
  lastUpdatedAt.value = new Date()
  refreshing.value = false
}

/**
 * 防抖刷新：连续收到多条事件时合并为一次重拉，避免高频写入压垮接口。
 *
 * <p>用户在学习资源的过程中可能连续触发 view / complete / quiz 多条记录，
 * 每次都重拉 /records + /evaluate 会浪费请求。200ms 已经能合并一次
 * 完整学习动作（打开 → 完成 → 答题）触发的所有埋点。
 */
function scheduleRefresh(): void {
  if (refreshTimer !== null) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(() => {
    refreshTimer = null
    void loadAll()
  }, 200)
}

/**
 * 订阅当前学生对应的实时事件。
 *
 * <p>同一学生只订阅一次：切换学生时先取消旧订阅再订阅新学生，避免
 * 重复订阅导致回调多次触发、图表渲染抖动。
 */
function subscribeRecordEvents(): void {
  if (!ws.connected.value) {
    ws.connect()
  }
  const currentId = studentId.value
  if (currentId === null) return

  const destination = `/topic/records/${currentId}`
  // 已订阅同一目的地：跳过
  if (subscribedDestination.value === destination) return
  // 切换学生：先退订旧订阅
  if (subscribedDestination.value !== null) {
    ws.unsubscribe(subscribedDestination.value)
    subscribedDestination.value = null
  }

  ws.subscribe(destination, (frame) => {
    let handled = true
    try {
      const payload = JSON.parse(frame.body) as { type?: string; studentId?: number }
      // 仅处理发给当前学生的事件：后端固定推到 /topic/records/{studentId}，
      // 但保险起见再校验一次 studentId，避免后端未来多人群发导致串数据。
      if (typeof payload.studentId === 'number' && payload.studentId !== currentId) {
        handled = false
      }
    } catch {
      // 解析失败仍走刷新，保持仪表盘最终一致
    }
    if (handled) {
      scheduleRefresh()
    }
  })
  subscribedDestination.value = destination
}

/** 取消订阅：仅清状态，不主动断开 ws（对话页可能还在用） */
function unsubscribeRecordEvents(): void {
  if (refreshTimer !== null) {
    window.clearTimeout(refreshTimer)
    refreshTimer = null
  }
  if (subscribedDestination.value !== null) {
    ws.unsubscribe(subscribedDestination.value)
    subscribedDestination.value = null
  }
}

function buildLineOption(): echarts.EChartsOption {
  const byDate: Record<string, number> = {}
  records.value.forEach((r) => {
    if (!isEffectiveLearningRecord(r)) return
    if (!r.createdAt) return
    const date = r.createdAt.slice(0, 10)
    byDate[date] = (byDate[date] ?? 0) + (r.duration ?? 0)
  })
  const dates = Object.keys(byDate).sort()
  const values = dates.map((d) => Math.round((byDate[d] ?? 0) / 60))
  return {
    title: { text: '每日学习时长（分钟）', left: 'left' },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: values,
        itemStyle: { color: '#409eff' },
        barWidth: '50%'
      }
    ]
  }
}

function buildBarOption(): echarts.EChartsOption {
  return {
    title: { text: '学习行为分布', left: 'left' },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: ['浏览', '完成', '答题'] },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: [reportViewCount.value, reportCompleteCount.value, reportQuizCount.value],
        itemStyle: {
          color: (params) => {
            const palette = ['#409eff', '#67c23a', '#e6a23c']
            return palette[params.dataIndex] ?? '#409eff'
          }
        },
        barWidth: '40%'
      }
    ]
  }
}

function renderCharts(): void {
  if (lineChartRef.value) {
    lineChart = lineChart ?? echarts.init(lineChartRef.value)
    lineChart.setOption(buildLineOption())
  }
  if (barChartRef.value) {
    barChart = barChart ?? echarts.init(barChartRef.value)
    barChart.setOption(buildBarOption())
  }
}

function onResize(): void {
  lineChart?.resize()
  barChart?.resize()
}

watch(studentId, (next, prev) => {
  if (next === null) {
    unsubscribeRecordEvents()
    return
  }
  if (next !== prev) {
    void loadAll()
  }
  subscribeRecordEvents()
})

onMounted(() => {
  void loadAll()
  subscribeRecordEvents()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  unsubscribeRecordEvents()
  lineChart?.dispose()
  barChart?.dispose()
  lineChart = null
  barChart = null
})

/** 把最近更新时间格式化为 HH:mm:ss，便于用户在仪表盘头部直观看到刷新情况 */
const lastUpdatedText = computed(() => {
  const t = lastUpdatedAt.value
  if (!t) return '尚未刷新'
  const pad = (n: number): string => n.toString().padStart(2, '0')
  return `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}`
})
</script>

<template>
  <div class="page-container dashboard">
    <div class="dashboard__header">
      <div class="dashboard__live" :class="{ 'dashboard__live--on': ws.connected.value, 'dashboard__live--busy': refreshing }">
        <span class="dashboard__live-dot" />
        <span class="dashboard__live-text">
          {{ refreshing ? '同步中…' : (ws.connected.value ? '实时跟新已连接' : '实时跟新未连接') }}
        </span>
        <span class="dashboard__live-time">最后刷新：{{ lastUpdatedText }}</span>
      </div>
    </div>

    <el-row :gutter="16" class="dashboard__metrics">
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="dashboard__metric">
          <div class="dashboard__metric-label">累计学习时长</div>
          <div class="dashboard__metric-value">{{ formatDuration(totalDuration) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="dashboard__metric">
          <div class="dashboard__metric-label">浏览资源</div>
          <div class="dashboard__metric-value">{{ totalViewed }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="dashboard__metric">
          <div class="dashboard__metric-label">完成资源</div>
          <div class="dashboard__metric-value">{{ totalCompleted }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="dashboard__metric">
          <div class="dashboard__metric-label">平均分</div>
          <div class="dashboard__metric-value">{{ formatScore(averageScore) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="dashboard__chart-card">
          <div ref="lineChartRef" class="dashboard__chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="dashboard__chart-card">
          <div ref="barChartRef" class="dashboard__chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="report" shadow="never" class="dashboard__report">
      <template #header>
        <div class="dashboard__report-header">
          <IconSvg name="clipboard" :size="16" /> 学习效果评估报告
        </div>
      </template>

      <section class="dashboard__report-section">
        <h4 class="dashboard__report-title">一、整体总览</h4>
        <p class="dashboard__report-text">{{ reportOverview }}</p>
      </section>

      <el-row :gutter="16" class="dashboard__report-stats">
        <el-col :xs="12" :md="6">
          <div class="dashboard__report-stat">
            <div class="dashboard__report-stat-label">学习记录</div>
            <div class="dashboard__report-stat-value">{{ reportTotalRecords }} 条</div>
          </div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="dashboard__report-stat">
            <div class="dashboard__report-stat-label">累计时长</div>
            <div class="dashboard__report-stat-value">{{ formatDuration(reportTotalDurationSeconds) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="dashboard__report-stat">
            <div class="dashboard__report-stat-label">完成率</div>
            <div class="dashboard__report-stat-value">{{ (reportCompletionRate * 100).toFixed(1) }}%</div>
          </div>
        </el-col>
        <el-col :xs="12" :md="6">
          <div class="dashboard__report-stat">
            <div class="dashboard__report-stat-label">答题平均分</div>
            <div class="dashboard__report-stat-value">{{ formatScore(reportAverageScore) }}</div>
          </div>
        </el-col>
      </el-row>

      <section class="dashboard__report-section">
        <h4 class="dashboard__report-title">二、亮点</h4>
        <ul class="dashboard__report-list dashboard__report-list--good">
          <li v-for="(item, idx) in reportHighlights" :key="`h-${idx}`">{{ item }}</li>
        </ul>
      </section>

      <section class="dashboard__report-section">
        <h4 class="dashboard__report-title">三、待改进</h4>
        <ul class="dashboard__report-list dashboard__report-list--warn">
          <li v-for="(item, idx) in reportConcerns" :key="`c-${idx}`">{{ item }}</li>
        </ul>
      </section>

      <section class="dashboard__report-section">
        <h4 class="dashboard__report-title">四、后续建议</h4>
        <ol class="dashboard__report-list dashboard__report-list--action">
          <li v-for="(item, idx) in reportSuggestions" :key="`s-${idx}`">{{ item }}</li>
        </ol>
      </section>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
  }
  &__header-text {
    flex: 1 1 auto;
  }
  &__live {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    border-radius: 999px;
    background: $border-extra-light;
    color: $text-secondary;
    font-size: 12px;
    line-height: 1;
    transition: background 0.2s ease, color 0.2s ease;
  }
  &__live-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $text-placeholder;
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.6);
    animation: dashboard__pulse 1.6s infinite;
  }
  &__live--on {
    background: rgba(103, 194, 58, 0.12);
    color: $success-color;
  }
  &__live--on &__live-dot {
    background: $success-color;
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.55);
  }
  &__live--busy {
    background: rgba(64, 158, 255, 0.12);
    color: $primary-color;
  }
  &__live-text {
    font-weight: 600;
  }
  &__live-time {
    color: inherit;
    opacity: 0.85;
    border-left: 1px solid currentColor;
    padding-left: 8px;
    margin-left: 2px;
  }
  &__metrics {
    margin-bottom: $spacing-md;
  }
  &__metric {
    text-align: center;
  }
  &__metric-label {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: $spacing-sm;
  }
  &__metric-value {
    font-size: 24px;
    font-weight: 600;
    color: $primary-color;
  }
  &__chart-card {
    margin-bottom: $spacing-md;
  }
  &__chart {
    width: 100%;
    height: 300px;
  }
  &__report {
    margin-top: -20px;
  }
  &__report-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
  &__report-section {
    margin-top: $spacing-sm;
    &:first-of-type {
      margin-top: 0;
    }
  }
  &__report-title {
    margin: 0 0 6px;
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
  }
  &__report-text {
    margin: 0;
    line-height: 1.6;
    color: $text-regular;
    font-size: 13px;
  }
  &__report-stats {
    margin-top: $spacing-sm;
    margin-bottom: $spacing-sm;
    padding: $spacing-sm;
    background: $border-extra-light;
    border-radius: $radius-sm;
  }
  &__report-stat {
    text-align: center;
    padding: $spacing-sm 0;
  }
  &__report-stat-label {
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 4px;
  }
  &__report-stat-value {
    font-size: 18px;
    font-weight: 600;
    color: $primary-color;
  }
  &__report-list {
    margin: 0;
    padding-left: 20px;
    line-height: 1.8;
    font-size: 13px;
    color: $text-regular;
  }
  &__report-list--good li::marker {
    color: $success-color;
  }
  &__report-list--warn li::marker {
    color: $warning-color;
  }
  &__report-list--action li::marker {
    color: $primary-color;
    font-weight: 600;
  }
}

@keyframes dashboard__pulse {
  0% {
    box-shadow: 0 0 0 0 currentColor;
    opacity: 1;
  }
  70% {
    box-shadow: 0 0 0 6px transparent;
    opacity: 0.6;
  }
  100% {
    box-shadow: 0 0 0 0 transparent;
    opacity: 1;
  }
}
</style>
