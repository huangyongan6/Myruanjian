<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  current: number
  total: number
  label?: string
  active?: boolean
}
const props = withDefaults(defineProps<Props>(), { active: false, label: '' })

const percent = computed(() => {
  if (props.total <= 0) return 0
  return Math.min(100, Math.round((props.current / props.total) * 100))
})
</script>

<template>
  <div v-if="active || percent > 0" class="progress-tracker">
    <div v-if="label" class="progress-tracker__label">{{ label }}</div>
    <el-progress
      :percentage="percent"
      :status="percent >= 100 ? 'success' : ''"
      :stroke-width="14"
      :show-text="true"
    />
  </div>
</template>

<style scoped lang="scss">
.progress-tracker {
  background: $bg-card;
  border: 1px solid $border-lighter;
  border-radius: $radius-md;
  padding: $spacing-md;
  &__label {
    font-size: 13px;
    color: $text-regular;
    margin-bottom: $spacing-sm;
  }
}
</style>
