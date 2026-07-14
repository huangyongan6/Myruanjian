# 前端说明（Vue 3）

> 本文档说明本项目前端部分的搭建、开发、模块划分和约束。前端位于仓库 `frontend/` 目录下，所有代码遵循 [CLAUDE.md](./CLAUDE.md) 的开发规范。

---

## 一、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架（Composition API + `<script setup>`） |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 / 开发服务器 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 全局状态管理 |
| Vue Router | 4.x | 路由 |
| Axios | 1.x | HTTP 客户端 |
| ECharts | 5.x | 雷达图、统计图 |
| markmap | — | 思维导图渲染 |
| Monaco Editor | — | 代码展示（只读） |
| markdown-it | — | Markdown 渲染 |

---

## 二、环境搭建

> 完整的开发环境配置（数据库、Redis、后端、前端）见 [dev-env.md](./dev-env.md)。本节仅列出前端特有的搭建步骤。

### 2.1 初始化

```bash
cd frontend
npm create vite@latest . -- --template vue-ts
npm install
```

### 2.2 关键依赖

```bash
npm install vue-router@4 pinia element-plus axios echarts markmap-view monaco-editor markdown-it highlight.js
npm install -D @types/markdown-it sass unplugin-auto-import unplugin-vue-components
```

### 2.3 启动脚本

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .vue,.ts,.tsx --fix",
    "type-check": "vue-tsc --noEmit"
  }
}
```

> 修改完代码后必须先 `npm run type-check` 或 `npm run build` 确认无类型错误，符合 CLAUDE.md 「3.4 做完后先编译」。

### 2.4 后端联调地址

前端开发服务器默认运行在 `http://localhost:5173`，通过 Vite 代理转发到后端 `http://localhost:8080`：

```bash
# frontend/.env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws
```

---

## 三、目录结构

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
└── src/
    ├── main.ts                     # 入口：注册 Pinia、Router、Element Plus
    ├── App.vue                     # 根组件
    ├── pages/                      # 页面级组件
    │   ├── ChatPage/               # 对话页（核心入口）
    │   ├── ProfilePage/            # 画像展示页
    │   ├── ResourceCenter/         # 资源中心
    │   ├── LearningPath/           # 学习路径页
    │   ├── TutorPage/              # 辅导页（加分）
    │   └── Dashboard/              # 学习仪表盘（加分）
    ├── components/                 # 可复用组件
    │   ├── ChatMessage/            # 聊天消息
    │   ├── MarkdownRenderer/       # Markdown 渲染
    │   ├── MindMapView/            # markmap 思维导图
    │   ├── QuizCard/               # 题目卡片
    │   ├── CodeViewer/             # Monaco 代码展示
    │   ├── ResourceCard/           # 5 种资源统一卡片
    │   ├── ProfileRadar/           # 6 维雷达图
    │   ├── PathTimeline/           # 学习路径时间线
    │   ├── ProgressTracker/        # 生成进度追踪
    │   └── RecommendPanel/         # 推荐面板
    ├── composables/                # 组合式函数
    │   ├── useWebSocket.ts         # WebSocket 连接 + 断线重连
    │   ├── useChat.ts              # 对话流式处理
    │   └── useResource.ts          # 资源生成/加载
    ├── stores/                     # Pinia 状态管理
    │   ├── chat.ts
    │   ├── profile.ts
    │   ├── resource.ts
    │   └── path.ts
    ├── services/                   # Axios API 封装
    │   ├── request.ts              # Axios 实例 + 拦截器
    │   ├── chat.ts
    │   ├── profile.ts
    │   ├── resource.ts
    │   └── path.ts
    ├── router/                     # 路由配置
    │   └── index.ts
    ├── types/                      # TypeScript 类型定义
    │   ├── student.ts
    │   ├── profile.ts
    │   ├── resource.ts
    │   └── path.ts
    └── utils/                      # 工具函数
        ├── format.ts
        └── storage.ts
```

---

## 四、命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `ChatPanel.vue` |
| 页面文件 | PascalCase | `ChatPage.vue` |
| 组合式函数 | `useXxx.ts` | `useWebSocket.ts` |
| 工具函数 | camelCase | `formatDate.ts` |
| 类型定义 | PascalCase / camelCase | `student.ts`、`resourceType.ts` |
| 组件名 | PascalCase | `<ChatPanel />` |
| 变量/函数 | camelCase | `studentId`、`fetchProfile` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| CSS 类名 | kebab-case（BEM） | `resource-card__title` |
| 路由路径 | kebab-case | `/resource-center` |

**禁止**：
- 拼音命名
- 无意义缩写（如 `stu`、`res`）

---

## 五、路由设计

`src/router/index.ts` 使用 Vue Router 4 配置：

```ts
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/pages/ChatPage/ChatPage.vue'),
    meta: { title: '对话学习' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage/ProfilePage.vue'),
    meta: { title: '学习画像' }
  },
  {
    path: '/resources',
    name: 'ResourceCenter',
    component: () => import('@/pages/ResourceCenter/ResourceCenter.vue'),
    meta: { title: '资源中心' }
  },
  {
    path: '/path',
    name: 'LearningPath',
    component: () => import('@/pages/LearningPath/LearningPath.vue'),
    meta: { title: '学习路径' }
  },
  {
    path: '/tutor',
    name: 'Tutor',
    component: () => import('@/pages/TutorPage/TutorPage.vue'),
    meta: { title: '智能辅导' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/Dashboard/Dashboard.vue'),
    meta: { title: '学习仪表盘' }
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
```

**要求**：
- 全部路由使用懒加载（`() => import('@/pages/...')`）
- 路由 name 必填，便于编程式导航
- 路由路径使用 kebab-case

---

## 六、状态管理（Pinia）

### 6.1 Store 划分

| Store | 职责 |
|-------|------|
| `useChatStore` | 对话消息、流式片段、当前 Agent |
| `useProfileStore` | 学生 6 维画像 |
| `useResourceStore` | 资源列表、当前查看的资源、生成进度 |
| `usePathStore` | 学习路径、当前步骤、进度 |
| `useStudentStore` | 当前学生信息 |

### 6.2 示例

```ts
// stores/profile.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StudentProfile } from '@/types/profile'
import { getProfile } from '@/services/profile'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<StudentProfile | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchProfile(studentId: number) {
    loading.value = true
    error.value = null
    try {
      const { data } = await getProfile(studentId)
      profile.value = data
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载画像失败'
    } finally {
      loading.value = false
    }
  }

  return { profile, loading, error, fetchProfile }
})
```

**要求**：
- API 状态三件套：`loading` / `error` / `data`
- 不在 Store 内直接操作 DOM
- 组件内部状态用 `ref` / `reactive`

---

## 七、API 调用

### 7.1 Axios 实例（拦截器）

`services/request.ts`：

```ts
import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60_000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default instance
```

### 7.2 业务 API

```ts
// services/profile.ts
import request from './request'
import type { StudentProfile } from '@/types/profile'

export function getProfile(studentId: number) {
  return request.get<unknown, { code: number; message: string; data: StudentProfile }>(
    `/profiles/${studentId}`
  )
}
```

**要求**：
- 统一封装，禁止组件内直接 `axios.get(...)`
- 错误通过拦截器统一 `ElMessage` 提示
- 文件名以业务实体命名，与后端 Controller 对齐

---

## 八、WebSocket 流式对话

### 8.1 连接

`composables/useWebSocket.ts`：

```ts
import { ref, onUnmounted } from 'vue'

export function useWebSocket(url: string) {
  const connected = ref(false)
  const messages = ref<WsMessage[]>([])
  let ws: WebSocket | null = null
  let retry = 0

  function connect() {
    ws = new WebSocket(url)
    ws.onopen = () => { connected.value = true; retry = 0 }
    ws.onmessage = (e) => messages.value.push(JSON.parse(e.data))
    ws.onclose = () => {
      connected.value = false
      // 指数退避重连（最多 5 次）
      if (retry < 5) {
        const delay = Math.min(1000 * 2 ** retry, 16_000)
        retry++
        setTimeout(connect, delay)
      }
    }
  }

  function send(payload: object) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload))
    }
  }

  onUnmounted(() => ws?.close())

  return { connected, messages, connect, send }
}
```

### 8.2 消息类型

后端约定 5 类消息（详见 [CLAUDE.md](./CLAUDE.md) §22）：

| type | 处理 |
|------|------|
| `message` | 追加到流式输出 buffer |
| `progress` | 更新 ProgressTracker |
| `resource` | 推送到 ResourceStore |
| `error` | `ElMessage.error` |
| `done` | 标记流式结束，触发 finalize |

---

## 九、5 种资源类型的渲染

| 类型 | type 值 | 渲染组件 |
|------|---------|---------|
| 课程讲解文档 | `doc` | `<MarkdownRenderer :content="resource.content.markdown" />` |
| 知识点思维导图 | `mindmap` | `<MindMapView :tree="resource.content.tree" />` |
| 练习题库 | `quiz` | `<QuizCard :questions="resource.content.questions" />` |
| 拓展阅读材料 | `reading` | 卡片列表 + 推荐理由 |
| 代码实操案例 | `code` | `<CodeViewer :code="resource.content.code" />` |

> 资源数据结构定义见 [CLAUDE.md](./CLAUDE.md) §21。

---

## 十、6 维画像的雷达图

使用 ECharts 实现：

```vue
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { StudentProfile } from '@/types/profile'

const props = defineProps<{ profile: StudentProfile }>()
const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!chartEl.value || !props.profile) return
  chart = chart ?? echarts.init(chartEl.value)
  chart.setOption({
    radar: {
      indicator: [
        { name: '数学基础', max: 100 },
        { name: '编程能力', max: 100 },
        { name: 'ML 熟悉度', max: 100 },
        { name: '学习目标', max: 100 },
        { name: '学习节奏', max: 100 },
        { name: '认知风格', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [{ value: [/* ... */], name: '学习画像' }]
    }]
  })
}

onMounted(render)
watch(() => props.profile, render)
</script>

<template>
  <div ref="chartEl" class="profile-radar" />
</template>

<style scoped>
.profile-radar {
  width: 100%;
  height: 400px;
}
</style>
```

---

## 十一、组件编写约定

### 11.1 必须使用 `<script setup lang="ts">`

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import type { StudentProfile } from '@/types/profile'

const props = defineProps<{ profile: StudentProfile }>()
const emit = defineEmits<{ (e: 'update', value: StudentProfile): void }>()

const displayName = computed(() => props.profile?.name ?? '未知')
</script>

<template>
  <div class="profile-card">
    <h3>{{ displayName }}</h3>
  </div>
</template>

<style scoped lang="scss">
.profile-card {
  padding: 16px;
}
</style>
```

### 11.2 组件拆分原则

- 单文件不超过 500 行（超过则拆分为子组件或 Composables）
- 单一职责：一个组件只做一件事
- 业务实体组件（如 `ResourceCard`）放 `components/`，页面级组件放 `pages/`
- 跨页面复用 → 提取为 Composables

---

## 十二、约束与禁止

| # | 规则 |
|---|------|
| 1 | 禁止直接操作 DOM（使用 Vue 响应式绑定） |
| 2 | 禁止提交 `console.log()` 调试代码 |
| 3 | 禁止在前端存储大段 AI 生成的原始 JSON（后端做精简后再返回） |
| 4 | 禁止使用拼音命名、无意义缩写 |
| 5 | 禁止在组件内直接 `axios` 调用，必须走 `services/` |
| 6 | 修改组件后必须先 `npm run type-check` 确认无类型错误 |
| 7 | 修改范围严格限定在本次任务内（CLAUDE.md §3.5） |

---

## 十三、提交前自检清单

- [ ] `npm run build` 无报错
- [ ] 所有路由懒加载
- [ ] 无 `console.log` / 调试代码残留
- [ ] 无未使用的 import / 变量
- [ ] 5 种资源均能在对应组件中正确渲染
- [ ] WebSocket 断线后能自动重连
- [ ] 浏览器控制台无报错、无警告
- [ ] 移动端 / 不同分辨率下布局正常