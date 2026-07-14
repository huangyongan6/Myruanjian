/**
 * 学习记录实体（对应后端 LearningRecord）。
 */
export interface LearningRecord {
  id: number
  studentId: number
  resourceId?: number
  action: 'view' | 'complete' | 'quiz'
  score?: number
  duration?: number
  createdAt?: string
}

/**
 * 记录请求体。
 */
export interface RecordRequest {
  studentId: number
  resourceId?: number
  action: 'view' | 'complete' | 'quiz'
  score?: number
  duration?: number
}

/**
 * 学习效果评估报告。
 *
 * <p>字段与后端 {@code LearningRecordServiceImpl#evaluate} 返回的 Map 对齐：
 * view / complete / quiz 三类行为计数、完成率、平均分、总学习时长等。
 * 额外保留弱项主题与改进建议，便于前端做"文字说明"渲染。
 */
export interface EvaluateReport {
  studentId?: number
  totalRecords?: number
  viewCount?: number
  completeCount?: number
  quizCount?: number
  completionRate?: number
  /** 平均分（quiz 行为统计，无答题时为 null） */
  averageScore?: number | null
  /** 累计学习时长（秒） */
  totalDurationSeconds?: number
  /** 累计学习时长（分钟），前端展示用 */
  totalDurationMinutes?: number
  /** 弱项主题（当前数据不足时为空数组） */
  weakTopics?: string[]
  /** 改进建议（当前数据不足时为空数组） */
  suggestions?: string[]
  [key: string]: unknown
}
