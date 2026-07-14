<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { RecommendedResource } from '@/types/recommend'
import type { LearningResource } from '@/types/resource'
import ResourceCard from '@/components/ResourceCard/ResourceCard.vue'
import {
  getResourceTypeLabel,
  getResourceTypeTagType,
  getDifficultyLabel,
  getDifficultyTagType
} from '@/utils/format'

interface Props {
  items: RecommendedResource[]
  emptyText?: string
  /** 当前学习路径步骤的知识点，用于高亮与排序上下文关联的资源。 */
  currentPoint?: string
}
const props = withDefaults(defineProps<Props>(), {
  emptyText: '暂无推荐资源',
  currentPoint: ''
})

const emit = defineEmits<{ (e: 'view', resourceId: number): void }>()

interface AnnotatedItem {
  raw: RecommendedResource
  resource: LearningResource
  isCurrentMatch: boolean
}

/**
 * 按当前路径知识点排序：命中的资源靠前展示，
 * 让推荐与"当前正在学的内容"保持上下文关联。
 */
const orderedItems = computed<AnnotatedItem[]>(() => {
  const point = props.currentPoint?.trim() ?? ''
  const annotated = props.items.map<AnnotatedItem>((item) => ({
    raw: item,
    resource: item.resource,
    isCurrentMatch:
      point.length > 0 &&
      !!item.resource.knowledgePoint &&
      item.resource.knowledgePoint.includes(point)
  }))
  annotated.sort((a, b) => {
    if (a.isCurrentMatch === b.isCurrentMatch) return 0
    return a.isCurrentMatch ? -1 : 1
  })
  return annotated
})

/** 已展开内容展示的资源 ID 集合。进入页面时自动展开匹配当前步骤的资源。 */
const expandedIds = ref<Set<number>>(new Set())

// 首次加载：把"当前步骤匹配"的资源默认展开；后续用户手动展开/收起不受影响。
let initDone = false
watch(
  () => orderedItems.value
    .filter(item => item.isCurrentMatch)
    .map(item => item.resource.id)
    .join(','),
  (ids) => {
    if (initDone) return
    const currentMatched = ids.split(',').filter(Boolean).map(Number)
    if (currentMatched.length > 0) {
      expandedIds.value = new Set(currentMatched)
      initDone = true
    }
  },
  { immediate: true }
)

function isExpanded(resource: LearningResource): boolean {
  return expandedIds.value.has(resource.id)
}

function toggle(resource: LearningResource): void {
  if (expandedIds.value.has(resource.id)) {
    expandedIds.value.delete(resource.id)
  } else {
    expandedIds.value.add(resource.id)
  }
  // 触发响应式更新
  expandedIds.value = new Set(expandedIds.value)
}

function onView(resource: LearningResource): void {
  emit('view', resource.id)
}

function onQuizSubmitted(percent: number): void {
  // 推荐面板里答题：静默打分即可，不阻断学习流程。
  // 后端的 quiz 行为埋点由资源中心负责；这里只做 UX 反馈。
  console.debug('[RecommendPanel] quiz submitted', percent)
}
</script>

<template>
  <div class="recommend-panel">
    <el-empty v-if="orderedItems.length === 0" :description="emptyText" />

    <el-card
      v-for="item in orderedItems"
      :key="item.resource.id"
      class="recommend-panel__item"
      :class="{ 'recommend-panel__item--matched': item.isCurrentMatch }"
      shadow="hover"
    >
      <div class="recommend-panel__head" @click="toggle(item.resource)">
        <div class="recommend-panel__head-left">
          <div class="recommend-panel__title">
            <span class="recommend-panel__name">
              {{ item.resource.title || getResourceTypeLabel(item.resource.type) }}
            </span>
            <el-tag
              size="small"
              :type="getResourceTypeTagType(item.resource.type)"
              effect="dark"
            >
              {{ getResourceTypeLabel(item.resource.type) }}
            </el-tag>
            <el-tag
              v-if="item.resource.difficulty"
              size="small"
              :type="getDifficultyTagType(item.resource.difficulty)"
              effect="plain"
            >
              {{ getDifficultyLabel(item.resource.difficulty) }}
            </el-tag>
            <el-tag v-if="item.isCurrentMatch" size="small" type="warning" effect="dark">
              📍 当前步骤
            </el-tag>
          </div>
          <div v-if="item.resource.knowledgePoint" class="recommend-panel__point">
            📘 {{ item.resource.knowledgePoint }}
          </div>
          <div v-if="item.raw.reason" class="recommend-panel__reason">
            💡 {{ item.raw.reason }}
          </div>
        </div>
        <div class="recommend-panel__head-right">
          <el-button
            size="small"
            text
            type="primary"
            @click.stop="onView(item.resource)"
          >
            详情
          </el-button>
          <el-button
            size="small"
            text
            :type="isExpanded(item.resource) ? 'primary' : 'default'"
            @click.stop="toggle(item.resource)"
          >
            {{ isExpanded(item.resource) ? '收起内容 ▲' : '展开内容 ▼' }}
          </el-button>
        </div>
      </div>

      <div v-if="isExpanded(item.resource)" class="recommend-panel__content">
        <ResourceCard
          :resource="item.resource"
          :show-detail="false"
          @quiz-submitted="onQuizSubmitted"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.recommend-panel {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;

  &__item {
    transition: transform 0.2s;
    &:hover { transform: translateY(-2px); }
  }

  &__item--matched {
    border: 1px solid $primary-color;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
  }

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: $spacing-sm;
    cursor: pointer;
  }

  &__head-left {
    flex: 1;
    min-width: 0;
  }

  &__head-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
    flex-shrink: 0;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-weight: 600;
    flex-wrap: wrap;
  }

  &__point,
  &__reason {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 4px;
  }
  &__reason { color: $primary-color; }

  &__content {
    margin-top: $spacing-md;
    padding-top: $spacing-md;
    border-top: 1px dashed $border-lighter;
  }
}
</style>