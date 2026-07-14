<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { Markmap } from 'markmap-view'
import type { MindMapNode } from '@/types/resource'

interface Props {
  tree: MindMapNode | null
}
const props = defineProps<Props>()

const containerRef = ref<HTMLDivElement | null>(null)
let markmap: Markmap | null = null

function render(): void {
  if (!containerRef.value) return
  if (!markmap) {
    // markmap-view 需要 SVG 容器；创建一个并附加到 div 中
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    containerRef.value.appendChild(svg)
    markmap = Markmap.create(svg, {
      // markmap-view 的 IMarkmapOptions 类型较新，这里用宽松选项
      ...({ maxWidth: 320 } as Record<string, unknown>),
      color: ((node: unknown) => {
        const depth = ((node as { state?: { depth?: number } }).state?.depth ?? 0) % 6
        const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9c64f0', '#13c2c2']
        return palette[depth] ?? '#409eff'
      }) as never
    })
  }
  const data = props.tree ?? ({ name: '暂无内容' } as MindMapNode)
  markmap.setData(data as never)
  markmap.fit()
}

onMounted(render)
watch(
  () => props.tree,
  () => render(),
  { deep: true }
)

onBeforeUnmount(() => {
  markmap?.destroy()
  markmap = null
})
</script>

<template>
  <div class="mind-map-view">
    <div v-if="!tree" class="empty-tip">未提供思维导图数据</div>
    <div v-else ref="containerRef" class="mind-map-view__container" />
  </div>
</template>

<style scoped lang="scss">
.mind-map-view {
  width: 100%;
  height: 100%;
  min-height: 400px;

  &__container {
    width: 100%;
    height: 500px;
    background: $bg-card;
    border-radius: $radius-md;
    overflow: hidden;
  }
}
</style>
