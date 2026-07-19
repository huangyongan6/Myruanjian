<script setup lang="ts">
import { computed } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer/MarkdownRenderer.vue'
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import MindMapView from '@/components/MindMapView/MindMapView.vue'
import QuizCard from '@/components/QuizCard/QuizCard.vue'
import CodeViewer from '@/components/CodeViewer/CodeViewer.vue'
import { parseDoc, parseMindMap, parseQuiz, parseReading, parseCode } from '@/stores/resource'
import { getResourceTypeLabel, getDifficultyLabel, getDifficultyTagType, formatDateTime, safeJsonParse } from '@/utils/format'
import type { LearningResource, DocContent, ReadingContent, ReadingItem } from '@/types/resource'

interface Props {
  resource: LearningResource
  showDetail?: boolean
}
const props = withDefaults(defineProps<Props>(), { showDetail: true })

const emit = defineEmits<{
  (e: 'view', resource: LearningResource): void
  (e: 'quiz-submitted', scorePercent: number): void
}>()

const docContent = computed(() => parseDoc(props.resource))
const mindMapContent = computed(() => parseMindMap(props.resource))
const quizContent = computed(() => parseQuiz(props.resource))
const readingContent = computed(() => parseReading(props.resource))
const codeContent = computed(() => parseCode(props.resource))

const isReading = computed(() => props.resource.type === 'reading')
const readingItems = computed<ReadingItem[]>(() => {
  if (!readingContent.value) return []
  return readingContent.value.items ?? []
})

const docSummary = computed<string>(() => docContent.value?.summary ?? '')

function onView(): void {
  emit('view', props.resource)
}

// 兼容 code 字段可能为 base64 / 普通文本
const codeText = computed<string>(() => {
  if (!codeContent.value) return ''
  return codeContent.value.code ?? ''
})

// 兼容 reading 单条 JSON-string 场景
const readingRawItems = computed<ReadingItem[]>(() => {
  if (isReading.value) {
    return safeJsonParse<ReadingItem[]>(props.resource.content, []) ?? []
  }
  return []
})
</script>

<template>
  <div class="resource-card">
    <div class="resource-card__header">
      <div class="resource-card__title-row">
        <h4 class="resource-card__title">{{ resource.title || getResourceTypeLabel(resource.type) }}</h4>
        <el-tag size="small" :type="getDifficultyTagType(resource.difficulty)" effect="plain">
          {{ getDifficultyLabel(resource.difficulty) }}
        </el-tag>
      </div>
      <div class="resource-card__meta">
        <span v-if="resource.knowledgePoint"><IconSvg name="tag" :size="14" /> {{ resource.knowledgePoint }}</span>
        <span><IconSvg name="clock" :size="14" /> {{ formatDateTime(resource.createdAt) }}</span>
      </div>
    </div>

    <div class="resource-card__body">
      <MarkdownRenderer v-if="resource.type === 'doc' && docContent" :content="docContent.markdown" />
      <div v-else-if="resource.type === 'doc'" class="empty-tip">暂无文档内容</div>

      <MindMapView
        v-else-if="resource.type === 'mindmap' && mindMapContent"
        :tree="mindMapContent.tree"
      />
      <div v-else-if="resource.type === 'mindmap'" class="empty-tip">暂无思维导图</div>

      <QuizCard
        v-else-if="resource.type === 'quiz' && quizContent"
        :questions="quizContent.questions"
        @submitted="(percent: number) => emit('quiz-submitted', percent)"
      />
      <div v-else-if="resource.type === 'quiz'" class="empty-tip">暂无题目</div>

      <div v-else-if="resource.type === 'reading'" class="resource-card__reading">
        <el-empty v-if="readingItems.length === 0 && readingRawItems.length === 0" description="暂无阅读材料" />
        <el-card
          v-for="(item, idx) in (readingItems.length > 0 ? readingItems : readingRawItems)"
          :key="idx"
          class="resource-card__reading-item"
          shadow="hover"
        >
          <div class="resource-card__reading-title">
            <span class="resource-card__reading-name">{{ item.title }}</span>
            <el-tag v-if="item.difficulty" size="small" effect="plain">
              {{ getDifficultyLabel(item.difficulty) }}
            </el-tag>
          </div>
          <div v-if="item.type" class="resource-card__reading-type"><IconSvg name="folder" :size="14" /> {{ item.type }}</div>
          <div v-if="item.reason" class="resource-card__reading-reason"><IconSvg name="idea" :size="14" /> {{ item.reason }}</div>
          <a v-if="item.url" :href="item.url" target="_blank" rel="noopener" class="resource-card__reading-link">
            查看原文 →
          </a>
        </el-card>
      </div>

      <div v-else-if="resource.type === 'code' && codeContent" class="resource-card__code">
        <p v-if="codeContent.description" class="resource-card__code-desc">
          <strong>说明：</strong>{{ codeContent.description }}
        </p>
        <CodeViewer :code="codeText" :language="codeContent.language ?? 'python'" />
        <div v-if="codeContent.explanation" class="resource-card__code-explain">
          <strong>讲解：</strong>{{ codeContent.explanation }}
        </div>
      </div>
      <div v-else-if="resource.type === 'code'" class="empty-tip">暂无代码案例</div>

      <div v-else class="empty-tip">未识别的资源类型：{{ resource.type }}</div>
    </div>

    <div v-if="showDetail" class="resource-card__footer">
      <el-button size="small" @click="onView">查看完整内容</el-button>
    </div>

    <div v-if="docSummary" class="resource-card__summary">摘要：{{ docSummary }}</div>
  </div>
</template>

<style scoped lang="scss">
.resource-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  transition: all $transition-normal;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  overflow: hidden;

  &:hover {
    box-shadow: $shadow-hover;
    border-color: $primary-color;
    transform: translateY(-1px);
  }

  &__header {
    border-bottom: 1px solid $border-lighter;
    padding-bottom: $spacing-md;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: $text-primary;
    letter-spacing: -0.01em;
  }

  &__meta {
    display: flex;
    gap: $spacing-lg;
    margin-top: $spacing-sm;
    font-size: 12px;
    color: $text-secondary;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  &__body {
    flex: 1;
    min-height: 100px;
  }

  &__mindmap {
    height: 450px;
  }

  &__reading {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__reading-item {
    border-radius: $radius-md;
    border: 1px solid $border-light;
    overflow: hidden;
    transition: all $transition-fast;

    &:hover {
      border-color: $primary-color;
      box-shadow: $shadow-sm;
    }

    :deep(.el-card__body) {
      padding: $spacing-md;
    }

    .resource-card__reading-title {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      font-weight: 600;
      font-size: 15px;
      color: $text-primary;
      margin-bottom: $spacing-xs;
    }

    .resource-card__reading-type,
    .resource-card__reading-reason {
      font-size: 12px;
      color: $text-secondary;
      margin-top: 4px;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .resource-card__reading-link {
      display: inline-flex;
      align-items: center;
      margin-top: $spacing-sm;
      font-size: 13px;
      font-weight: 500;
      color: $primary-color;
      transition: all $transition-fast;

      &:hover {
        transform: translateX(4px);
      }
    }
  }

  &__code {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;

    &-desc {
      margin: 0;
      color: $text-regular;
      font-size: 14px;
      line-height: 1.6;
    }

    &-explain {
      padding: $spacing-md;
      background: rgba(59, 130, 246, 0.05);
      border-left: 3px solid $primary-color;
      border-radius: 0 $radius-sm $radius-sm 0;
      font-size: 13px;
      color: $text-regular;
      line-height: 1.6;
    }
  }

  &__footer {
    text-align: right;
    padding-top: $spacing-sm;
    border-top: 1px dashed $border-lighter;
  }

  &__summary {
    font-size: 13px;
    color: $text-secondary;
    background: $border-lighter;
    padding: $spacing-md;
    border-radius: $radius-md;
    line-height: 1.5;
  }
}
</style>
