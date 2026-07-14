/**
 * 5 种资源类型枚举。
 */
export type ResourceType = 'doc' | 'mindmap' | 'quiz' | 'reading' | 'code'

export const RESOURCE_TYPE_LIST: ResourceType[] = ['doc', 'mindmap', 'quiz', 'reading', 'code']

/**
 * 课程讲解文档内容。
 */
export interface DocContent {
  markdown: string
  summary?: string
}

/**
 * 知识点思维导图内容。
 */
export interface MindMapNode {
  content: string
  children?: MindMapNode[]
}

export interface MindMapContent {
  tree: MindMapNode
}

/**
 * 练习题库内容。
 */
export interface QuizQuestion {
  type: 'single' | 'multiple' | 'truefalse' | 'short'
  question: string
  options?: string[]
  answer: string | string[]
  explanation?: string
}

export interface QuizContent {
  questions: QuizQuestion[]
}

/**
 * 拓展阅读材料内容。
 */
export interface ReadingItem {
  title: string
  url?: string
  type?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  reason?: string
}

export interface ReadingContent {
  items: ReadingItem[]
}

/**
 * 代码实操案例内容。
 */
export interface CodeContent {
  description: string
  dataset?: string
  code: string
  language?: string
  expected_output?: string
  explanation?: string
}

/**
 * 学习资源实体（对应后端 LearningResource）。
 *
 * <p>content 字段后端以 String-JSON 存储，前端根据 type 用 JSON.parse 反序列化为对应内容结构。
 */
export interface LearningResource {
  id: number
  studentId: number
  type: ResourceType
  title: string
  content: string
  knowledgePoint?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  createdAt?: string
}

/**
 * 资源生成请求体。
 */
export interface GenerateResourceRequest {
  studentId: number
  type: ResourceType
  knowledgePoint: string
}
