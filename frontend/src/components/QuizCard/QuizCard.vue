<script setup lang="ts">
import { ref, computed } from 'vue'
import type { QuizQuestion } from '@/types/resource'

interface Props {
  questions: QuizQuestion[]
}
const props = defineProps<Props>()

const emit = defineEmits<{ (e: 'submitted', scorePercent: number): void }>()

interface AnswerState {
  selected: string[]
  textAnswer: string
  revealed: boolean
}

const activeNames = ref<string[]>([])
const answers = ref<Record<number, AnswerState>>({})
const submitted = ref(false)

function ensureAnswer(index: number): AnswerState {
  if (!answers.value[index]) {
    answers.value[index] = { selected: [], textAnswer: '', revealed: false }
  }
  return answers.value[index]!
}

function isMulti(question: QuizQuestion): boolean {
  return question.type === 'multiple'
}

function isShort(question: QuizQuestion): boolean {
  return question.type === 'short'
}

function isOptionSelected(qIndex: number, option: string): boolean {
  return ensureAnswer(qIndex).selected.includes(option)
}

function toggleOption(qIndex: number, option: string): void {
  if (submitted.value) return
  const ans = ensureAnswer(qIndex)
  const question = props.questions[qIndex]
  if (!question) return
  if (isMulti(question)) {
    const idx = ans.selected.indexOf(option)
    if (idx >= 0) ans.selected.splice(idx, 1)
    else ans.selected.push(option)
  } else {
    ans.selected = [option]
  }
}

function isCorrectAnswer(question: QuizQuestion, option: string): boolean {
  if (Array.isArray(question.answer)) return question.answer.includes(option)
  return question.answer === option
}

function isAnswerCorrect(question: QuizQuestion, ans: AnswerState): boolean {
  const correctAnswers = Array.isArray(question.answer) ? question.answer : [question.answer]
  if (correctAnswers.length !== ans.selected.length) return false
  return correctAnswers.every((a) => ans.selected.includes(a))
}

const score = computed(() => {
  if (!submitted.value) return 0
  let correct = 0
  props.questions.forEach((q, idx) => {
    const ans = answers.value[idx]
    if (ans && isAnswerCorrect(q, ans)) correct += 1
  })
  return correct
})

const scorePercent = computed(() => {
  if (props.questions.length === 0) return 0
  return Math.round((score.value / props.questions.length) * 100)
})

function submit(): void {
  submitted.value = true
  props.questions.forEach((q, idx) => {
    const ans = ensureAnswer(idx)
    ans.revealed = true
  })
  emit('submitted', scorePercent.value)
}

function reset(): void {
  answers.value = {}
  submitted.value = false
  activeNames.value = []
}
</script>

<template>
  <div class="quiz-card">
    <div v-if="submitted" class="quiz-card__score">
      <el-result
        :icon="scorePercent >= 60 ? 'success' : 'warning'"
        :title="`得分：${score} / ${questions.length}（${scorePercent}%）`"
        :sub-title="scorePercent >= 60 ? '掌握良好，继续保持' : '建议复习相关知识点'"
      >
        <template #extra>
          <el-button type="primary" @click="reset">重新作答</el-button>
        </template>
      </el-result>
    </div>

    <el-collapse v-model="activeNames" class="quiz-card__list">
      <el-collapse-item
        v-for="(q, index) in questions"
        :key="index"
        :name="String(index)"
        :title="`第 ${index + 1} 题：${q.question}`"
      >
        <template #title>
          <div class="quiz-card__title">
            <span class="quiz-card__index">第 {{ index + 1 }} 题</span>
            <el-tag size="small" :type="isMulti(q) ? 'warning' : isShort(q) ? 'success' : 'info'">
              {{ isMulti(q) ? '多选' : isShort(q) ? '简答' : q.type === 'truefalse' ? '判断' : '单选' }}
            </el-tag>
            <span class="quiz-card__question">{{ q.question }}</span>
          </div>
        </template>

        <!-- 简答题：显示输入框 -->
        <div v-if="isShort(q)" class="quiz-card__short">
          <el-input
            v-model="ensureAnswer(index).textAnswer"
            type="textarea"
            :rows="3"
            placeholder="请输入你的答案..."
            :disabled="submitted"
          />
        </div>

        <!-- 选择题：显示选项 -->
        <div v-else class="quiz-card__options">
          <div
            v-for="option in q.options ?? []"
            :key="option"
            class="quiz-card__option"
            :class="{
              'is-selected': isOptionSelected(index, option),
              'is-correct': submitted && isCorrectAnswer(q, option),
              'is-wrong': submitted && isOptionSelected(index, option) && !isCorrectAnswer(q, option)
            }"
            @click="toggleOption(index, option)"
          >
            {{ option }}
          </div>
        </div>

        <div v-if="submitted" class="quiz-card__explanation">
          <template v-if="isShort(q)">
            <div class="quiz-card__answer-box">
              <strong>参考答案：</strong>
              <p class="quiz-card__answer-text">{{ Array.isArray(q.answer) ? q.answer.join('') : q.answer }}</p>
            </div>
          </template>
          <template v-else>
            <strong>正确答案：</strong>
            <span>{{ Array.isArray(q.answer) ? q.answer.join('、') : q.answer }}</span>
          </template>
          <div v-if="q.explanation" class="quiz-card__explanation-text">
            <strong>解析：</strong>{{ q.explanation }}
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <div v-if="!submitted" class="quiz-card__actions">
      <el-button type="primary" :disabled="Object.keys(answers).length === 0" @click="submit">
        提交答案
      </el-button>
      <el-button @click="reset">重置</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.quiz-card {
  &__title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    width: 100%;
    flex-wrap: wrap;
  }

  &__index {
    font-weight: 600;
    color: $text-primary;
    font-size: 13px;
    background: $border-lighter;
    padding: 4px 10px;
    border-radius: $radius-full;
  }

  &__question {
    color: $text-regular;
    font-size: 14px;
    flex: 1;
    line-height: 1.5;
  }

  &__options {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
    padding: $spacing-md 0;
  }

  &__option {
    padding: $spacing-md;
    border: 1.5px solid $border-light;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all $transition-fast;
    font-size: 14px;
    line-height: 1.6;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: transparent;
      transition: background $transition-fast;
    }

    &:hover {
      border-color: $primary-color;
      background: rgba(59, 130, 246, 0.03);
      transform: translateX(4px);
    }

    &.is-selected {
      background: rgba(59, 130, 246, 0.08);
      border-color: $primary-color;
      color: $primary-color;
      font-weight: 500;

      &::before {
        background: $primary-color;
      }
    }

    &.is-correct {
      background: rgba(16, 185, 129, 0.08);
      border-color: $success-color;
      color: $success-color;
      font-weight: 500;

      &::before {
        background: $success-color;
      }
    }

    &.is-wrong {
      background: rgba(239, 68, 68, 0.08);
      border-color: $danger-color;
      color: $danger-color;
      font-weight: 500;

      &::before {
        background: $danger-color;
      }
    }
  }

  &__explanation {
    margin-top: $spacing-lg;
    padding: $spacing-lg;
    background: $border-lighter;
    border-radius: $radius-md;
    font-size: 14px;
    color: $text-regular;
    line-height: 1.6;
    border-left: 3px solid $primary-color;

    &-text {
      margin-top: $spacing-md;
      color: $text-secondary;
      font-size: 13px;
    }
  }

  &__short {
    padding: $spacing-md 0;

    :deep(.el-input__inner) {
      border-radius: $radius-md;
      border-color: $border-light;
      font-size: 14px;
      line-height: 1.6;
    }
  }

  &__answer-box {
    margin-bottom: $spacing-md;
  }

  &__answer-text {
    margin: $spacing-sm 0 0;
    color: $success-color;
    font-size: 14px;
    font-weight: 500;
    padding: $spacing-sm $spacing-md;
    background: rgba(16, 185, 129, 0.08);
    border-radius: $radius-sm;
    display: inline-block;
  }

  &__actions {
    margin-top: $spacing-xl;
    text-align: center;
    display: flex;
    justify-content: center;
    gap: $spacing-md;

    :deep(.el-button) {
      border-radius: $radius-md;
      font-weight: 500;
      padding: 10px 24px;
    }
  }

  &__score {
    margin-bottom: $spacing-lg;

    :deep(.el-result) {
      background: $bg-card;
      border-radius: $radius-lg;
      border: 1px solid $border-light;
    }
  }
}
</style>
