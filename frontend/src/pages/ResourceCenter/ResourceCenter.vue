<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ResourceCard from '@/components/ResourceCard/ResourceCard.vue'
import ProgressTracker from '@/components/ProgressTracker/ProgressTracker.vue'
import { useStudentStore } from '@/stores/student'
import { getResourceTypeLabel, getResourceTypeTagType, getAllResourceTypes } from '@/utils/format'
import { listMockResources } from '@/services/mockResources'
import { trackAction } from '@/services/record'
import type { ResourceType, LearningResource } from '@/types/resource'

/**
 * 资源中心页面（Mock 数据版）。
 *
 * <p>当前阶段优先用前端 Mock 数据展示 5 种资源类型的完整内容，不依赖后端 AI 调用。
 * 切回真实接口时，把 {@link loadResources} 改为 {@code useResource().loadList} 即可。
 */
const studentStore = useStudentStore()

const resources = ref<LearningResource[]>([])
const loading = ref(false)
const generationProgress = ref<{ current: number; total: number; label: string; active: boolean } | null>(null)

const filterType = ref<ResourceType | 'all'>('all')
const detailVisible = ref(false)
const detailResource = ref<LearningResource | null>(null)
/** 详情弹窗打开时间戳（ms），用于关闭时结算停留时长 */
let viewStartAt = 0
const generateDialog = ref(false)
const generateType = ref<ResourceType>('doc')
const generatePoint = ref('')

const studentId = computed(() => studentStore.currentStudentId)
const typeFilters = [
  { label: '全部', value: 'all' },
  ...getAllResourceTypes().map((t) => ({ label: getResourceTypeLabel(t), value: t }))
]

const filteredResources = computed(() => {
  if (filterType.value === 'all') return resources.value
  return resources.value.filter((r) => r.type === filterType.value)
})

/**
 * 加载资源列表：当前使用 Mock 数据；真实接口接入后替换为 useResource().loadList。
 */
async function loadResources(sid: number, type?: ResourceType): Promise<void> {
  loading.value = true
  try {
    // 模拟网络延迟，便于观察加载态
    await new Promise((resolve) => setTimeout(resolve, 200))
    resources.value = listMockResources(sid, type)
  } finally {
    loading.value = false
  }
}

async function reload(): Promise<void> {
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  await loadResources(studentId.value, filterType.value === 'all' ? undefined : filterType.value)
}

async function openGenerate(): Promise<void> {
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  generateDialog.value = true
}

/**
 * Mock 阶段的"生成"动作：从现有数据中找一条同类型的资源作为模板，
 * 复制并修改标题/知识点后插入到列表头，模拟新生成效果。
 */
async function confirmGenerate(): Promise<void> {
  if (!generatePoint.value.trim()) {
    ElMessage.warning('请输入知识点')
    return
  }
  if (studentId.value === null) return

  const point = generatePoint.value.trim()
  const type = generateType.value
  const template = resources.value.find((r) => r.type === type) ?? resources.value[0]
  if (!template) {
    ElMessage.warning('暂无模板可生成，请先刷新列表')
    return
  }

  generationProgress.value = { current: 1, total: 3, label: '正在调起 Agent...', active: true }
  await new Promise((r) => setTimeout(r, 400))
  generationProgress.value = { current: 2, total: 3, label: '正在生成内容...', active: true }
  await new Promise((r) => setTimeout(r, 600))
  generationProgress.value = { current: 3, total: 3, label: '正在落库...', active: true }
  await new Promise((r) => setTimeout(r, 300))

  const newResource: LearningResource = {
    ...template,
    id: Date.now(),
    studentId: studentId.value,
    type,
    title: `${point} ${getResourceTypeLabel(type)}`,
    knowledgePoint: point,
    createdAt: new Date().toISOString()
  }
  resources.value = [newResource, ...resources.value]
  generationProgress.value = { current: 3, total: 3, label: '生成完成', active: false }

  generateDialog.value = false
  generatePoint.value = ''
  ElMessage.success('生成完成（Mock）')
  setTimeout(() => {
    generationProgress.value = null
  }, 1500)
}

function viewDetail(resource: LearningResource): void {
  detailResource.value = resource
  detailVisible.value = true
  // 打开即记一条浏览行为，并开始计时
  viewStartAt = Date.now()
  if (studentId.value !== null) {
    void trackAction({ studentId: studentId.value, action: 'view' })
  }
}

/**
 * 关闭详情弹窗：结算停留时长，记一条带 duration 的浏览行为。
 */
function onDetailClose(): void {
  clearProgress()
  if (studentId.value !== null && viewStartAt > 0) {
    const duration = Math.round((Date.now() - viewStartAt) / 1000)
    if (duration > 0) {
      void trackAction({ studentId: studentId.value, action: 'view', duration })
    }
  }
  viewStartAt = 0
}

/**
 * 标记当前资源为已完成。
 */
function markCompleted(): void {
  if (studentId.value === null) {
    ElMessage.warning('请先选择学生')
    return
  }
  void trackAction({ studentId: studentId.value, action: 'complete' })
  ElMessage.success('已标记完成')
}

/**
 * 答题提交回调：记一条 quiz 行为，score 为正确率百分比（0~100）。
 */
function onQuizSubmitted(percent: number): void {
  if (studentId.value === null) return
  void trackAction({ studentId: studentId.value, action: 'quiz', score: percent })
}

function clearProgress(): void {
  generationProgress.value = null
}

onMounted(reload)
</script>

<template>
  <div class="page-container resource-center">
    <h2 class="page-title">资源中心</h2>
    <p class="page-subtitle">5 种个性化学习资源：课程讲解 / 思维导图 / 练习题库 / 拓展阅读 / 代码实操。</p>

    <div class="resource-center__toolbar">
      <el-radio-group v-model="filterType" @change="reload">
        <el-radio-button v-for="f in typeFilters" :key="f.value" :value="f.value">
          {{ f.label }}
        </el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="openGenerate">✨ 搜索资源</el-button>
    </div>

    <ProgressTracker
      v-if="generationProgress?.active"
      :current="generationProgress.current"
      :total="generationProgress.total"
      :label="generationProgress.label"
      :active="true"
    />

    <div v-if="loading && resources.length === 0" class="empty-tip">加载中...</div>
    <el-empty v-else-if="filteredResources.length === 0" description="暂无资源，点击右上角生成" />

    <el-row v-else :gutter="16">
      <el-col v-for="r in filteredResources" :key="r.id" :xs="24" :sm="12" :md="8" :lg="6" class="resource-center__col">
        <el-card class="resource-center__card" shadow="hover">
          <div class="resource-center__card-head">
            <el-tag :type="getResourceTypeTagType(r.type)" effect="dark" size="small">
              {{ getResourceTypeLabel(r.type) }}
            </el-tag>
            <span class="resource-center__date">{{ r.createdAt?.slice(0, 10) }}</span>
          </div>
          <h4 class="resource-center__title">{{ r.title || getResourceTypeLabel(r.type) }}</h4>
          <p v-if="r.knowledgePoint" class="resource-center__point">📘 {{ r.knowledgePoint }}</p>
          <el-button class="resource-center__open" size="small" type="primary" plain @click="viewDetail(r)">
            查看详情
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="detailVisible"
      :title="detailResource ? getResourceTypeLabel(detailResource.type) : '资源详情'"
      width="900px"
      top="5vh"
      destroy-on-close
      @close="onDetailClose"
    >
      <ResourceCard
        v-if="detailResource"
        :resource="detailResource"
        :show-detail="false"
        @quiz-submitted="onQuizSubmitted"
      />
      <template #footer>
        <el-button type="success" @click="markCompleted">✓ 标记完成</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generateDialog" title="搜索资源" width="480px">
      <el-form label-width="80px">
        <el-form-item label="资源类型">
          <el-select v-model="generateType" style="width: 100%">
            <el-option v-for="t in getAllResourceTypes()" :key="t" :label="getResourceTypeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="generatePoint" placeholder="例如：线性回归" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="confirmGenerate">立即搜索</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.resource-center {
  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
    flex-wrap: wrap;
    gap: $spacing-sm;
  }
  &__col { margin-bottom: $spacing-md; }
  &__card {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  &__card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-sm;
  }
  &__date {
    font-size: 12px;
    color: $text-secondary;
  }
  &__title {
    margin: 0 0 $spacing-sm;
    font-size: 15px;
    color: $text-primary;
  }
  &__point {
    margin: 0 0 $spacing-md;
    font-size: 12px;
    color: $text-secondary;
  }
  &__open { align-self: flex-start; }
}
</style>
