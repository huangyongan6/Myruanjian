<script setup lang="ts">
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import type { PathStep } from '@/types/path'

interface Props {
  steps: PathStep[]
  currentIndex: number
}
const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'complete', index: number): void }>()

function stepState(index: number): 'finish' | 'process' | 'wait' {
  if (index < props.currentIndex) return 'finish'
  if (index === props.currentIndex) return 'process'
  return 'wait'
}

function stepTimestamp(index: number): string {
  if (index < props.currentIndex) return '已完成'
  if (index === props.currentIndex) return '进行中'
  return '未开始'
}
</script>

<template>
  <div class="path-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(step, index) in steps"
        :key="step.index ?? index"
        :timestamp="stepTimestamp(index)"
        :type="stepState(index) === 'finish' ? 'success' : stepState(index) === 'process' ? 'primary' : 'info'"
        :hollow="stepState(index) !== 'process'"
        placement="top"
      >
        <el-card class="path-timeline__card" shadow="hover">
          <div class="path-timeline__title">
            <span class="path-timeline__index">步骤 {{ index + 1 }}</span>
            <h4 class="path-timeline__name">{{ step.title }}</h4>
          </div>
          <p v-if="step.description" class="path-timeline__desc">{{ step.description }}</p>
          <div class="path-timeline__meta">
            <el-tag v-if="step.knowledgePoint" size="small" type="info" effect="plain">
              <IconSvg name="tag" :size="12" /> {{ step.knowledgePoint }}
            </el-tag>
            <el-tag v-if="step.estimatedMinutes" size="small" type="warning" effect="plain">
              <IconSvg name="clock" :size="12" /> 约 {{ step.estimatedMinutes }} 分钟
            </el-tag>
            <el-tag v-if="step.resourceType" size="small" effect="plain">
              <IconSvg name="box" :size="12" /> {{ step.resourceType }}
            </el-tag>
          </div>
          <div class="path-timeline__actions">
            <el-button
              v-if="index === currentIndex"
              size="small"
              type="primary"
              @click="emit('complete', index)"
            >
              标记为已完成
            </el-button>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="steps.length === 0" description="暂无学习路径，点击下方按钮生成" />
  </div>
</template>

<style scoped lang="scss">
.path-timeline {
  :deep(.el-timeline-item__node) {
    font-size: 14px;
  }

  :deep(.el-timeline-item__content) {
    padding-top: 4px;
  }

  &__card {
    margin-bottom: $spacing-md;
    border-radius: $radius-lg;
    border: 1px solid $border-light;
    transition: all $transition-normal;
    overflow: hidden;

    &:hover {
      box-shadow: $shadow-hover;
      border-color: $primary-color;
    }

    :deep(.el-card__body) {
      padding: $spacing-lg;
    }
  }

  &__title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
  }

  &__index {
    font-size: 12px;
    color: $text-secondary;
    font-weight: 500;
    background: $border-lighter;
    padding: 4px 10px;
    border-radius: $radius-full;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__name {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    letter-spacing: -0.01em;
  }

  &__desc {
    margin: $spacing-md 0;
    color: $text-regular;
    line-height: 1.7;
    font-size: 14px;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    margin-top: $spacing-md;
    padding-top: $spacing-md;
    border-top: 1px solid $border-lighter;
  }

  &__actions {
    margin-top: $spacing-lg;
    text-align: right;

    :deep(.el-button) {
      border-radius: $radius-md;
      font-weight: 500;
      padding: 8px 20px;
    }
  }
}
</style>
