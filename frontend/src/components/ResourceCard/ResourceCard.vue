<script setup lang="ts">
import { computed } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer/MarkdownRenderer.vue'
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
        <span v-if="resource.knowledgePoint">📘 {{ resource.knowledgePoint }}</span>
        <span>🕒 {{ formatDateTime(resource.createdAt) }}</span>
      </div>
    </div>

    <div class="resource-card__body">
      <MarkdownRenderer v-if="resource.type === 'doc' && docContent" :content="docContent.markdown" />
      <div v-else-if="resource.type === 'doc'" class="empty-tip">暂无文档内容</div>

      <div v-else-if="resource.type === 'mindmap' && mindMapContent" class="resource-card__mindmap">
        <MindMapView :tree="mindMapContent.tree" />
      </div>
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
          <div v-if="item.type" class="resource-card__reading-type">📂 {{ item.type }}</div>
          <div v-if="item.reason" class="resource-card__reading-reason">💡 {{ item.reason }}</div>
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
  border: 1px solid $border-lighter;
  border-radius: $radius-md;
  padding: $spacing-md;
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;

  &:hover {
    box-shadow: $shadow-hover;
  }

  &__header {
    border-bottom: 1px solid $border-lighter;
    padding-bottom: $spacing-sm;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  &__title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }

  &__meta {
    display: flex;
    gap: $spacing-md;
    margin-top: $spacing-xs;
    font-size: 12px;
    color: $text-secondary;
  }

  &__body {
    flex: 1;
  }

  &__mindmap {
    height: 500px;
  }

  &__reading {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }

  &__reading-item {
    .resource-card__reading-title {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      font-weight: 600;
    }
    .resource-card__reading-type,
    .resource-card__reading-reason {
      font-size: 12px;
      color: $text-secondary;
      margin-top: 4px;
    }
    .resource-card__reading-link {
      display: inline-block;
      margin-top: $spacing-sm;
      font-size: 13px;
    }
  }

  &__code {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
    &-desc { margin: 0; color: $text-regular; }
    &-explain {
      padding: $spacing-sm $spacing-md;
      background: $border-extra-light;
      border-radius: $radius-sm;
      font-size: 13px;
      color: $text-regular;
    }
  }

  &__footer {
    text-align: right;
  }

  &__summary {
    font-size: 12px;
    color: $text-secondary;
    background: $border-extra-light;
    padding: $spacing-sm $spacing-md;
    border-radius: $radius-sm;
  }
}
</style>
