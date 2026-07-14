/**
 * 6 维画像子结构。
 *
 * <p>后端以 String-JSON 存储，前端读取后用 JSON.parse 解析对应字段。
 */
export interface KnowledgeBase {
  math_level?: number
  programming_level?: number
  ml_familiarity?: number
}

export interface CognitiveStyle {
  visual?: number
  textual?: number
  hands_on?: number
}

export interface LearningGoal {
  goal_type?: string
  target_direction?: string
}

export interface WeakPoints {
  weak_topics?: string[]
  mistake_types?: string[]
}

export interface LearningPace {
  daily_hours?: number
  pace?: string
}

export interface InterestArea {
  areas?: string[]
  preferred_project_type?: string
}

/**
 * 学生画像实体（对应后端 StudentProfile）。
 */
export interface StudentProfile {
  id?: number
  studentId: number
  knowledgeBase?: string
  cognitiveStyle?: string
  learningGoal?: string
  weakPoints?: string
  learningPace?: string
  interestArea?: string
  updatedAt?: string
}

/**
 * 解析后的画像（前端使用版本）。
 */
export interface ParsedProfile {
  studentId: number
  knowledgeBase: KnowledgeBase
  cognitiveStyle: CognitiveStyle
  learningGoal: LearningGoal
  weakPoints: WeakPoints
  learningPace: LearningPace
  interestArea: InterestArea
  updatedAt?: string
}

/**
 * 触发画像生成的请求体。
 */
export interface GenerateProfileRequest {
  content: string
}
