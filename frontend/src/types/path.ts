/**
 * 学习路径步骤（解析自 pathData JSON）。
 */
export interface PathStep {
  index: number
  title: string
  description?: string
  knowledgePoint?: string
  resourceType?: string
  estimatedMinutes?: number
  completed?: boolean
}

/**
 * 路径数据（pathData 解析后结构）。
 */
export interface PathData {
  steps: PathStep[]
}

/**
 * 学习路径实体（对应后端 LearningPath）。
 */
export interface LearningPath {
  id: number
  studentId: number
  totalSteps: number
  currentStep: number
  pathData: string
  updatedAt?: string
}

/**
 * 触发路径规划请求体。
 */
export interface GeneratePathRequest {
  studentId: number
}
