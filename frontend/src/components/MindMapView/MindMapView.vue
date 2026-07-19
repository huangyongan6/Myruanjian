<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { MindMapNode } from '@/types/resource'

interface Props {
  tree: MindMapNode | null | undefined
}
const props = defineProps<Props>()

const expandedNodes = ref<Set<string>>(new Set())

function toggleNode(path: string): void {
  if (expandedNodes.value.has(path)) {
    expandedNodes.value.delete(path)
  } else {
    expandedNodes.value.add(path)
  }
}

function isExpanded(path: string): boolean {
  return expandedNodes.value.has(path)
}

function expandAll(node: MindMapNode, path: string, depth: number): void {
  if (depth < 2 && node.children?.length) {
    expandedNodes.value.add(path)
    node.children.forEach((child, idx) => {
      expandAll(child, `${path}-${idx}`, depth + 1)
    })
  }
}

onMounted(() => {
  if (props.tree) {
    expandAll(props.tree, 'root', 0)
  }
})

watch(() => props.tree, (newTree) => {
  expandedNodes.value.clear()
  if (newTree) {
    expandAll(newTree, 'root', 0)
  }
}, { immediate: true })
</script>

<template>
  <div class="mind-map">
    <div v-if="!tree" class="mind-map__empty">暂无思维导图数据，请先选择知识点生成资源</div>
    <div v-else class="mind-map__container">
      <!-- 根节点 -->
      <div class="mind-map__level mind-map__level--root">
        <div class="mind-map__node mind-map__node--root">
          {{ tree.content }}
        </div>
      </div>

      <!-- 第二层节点 -->
      <div v-if="tree.children?.length" class="mind-map__level mind-map__level--1">
        <div
          v-for="(child, idx) in tree.children"
          :key="idx"
          class="mind-map__branch"
        >
          <div
            class="mind-map__node mind-map__node--parent"
            :class="{ 'is-expandable': child.children?.length }"
            @click="child.children?.length && toggleNode(`root-${idx}`)"
          >
            <span class="mind-map__node-text">{{ child.content }}</span>
            <span v-if="child.children?.length" class="mind-map__toggle">
              {{ isExpanded(`root-${idx}`) ? '−' : '+' }}
            </span>
          </div>

          <!-- 第三层节点 -->
          <div v-if="isExpanded(`root-${idx}`) && child.children?.length" class="mind-map__children">
            <div
              v-for="(grandchild, gIdx) in child.children"
              :key="gIdx"
              class="mind-map__sub-branch"
            >
              <div class="mind-map__node mind-map__node--child">
                {{ grandchild.content }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mind-map {
  width: 100%;
  min-height: 400px;
  overflow: auto;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  border: 1px solid $border-light;
  transition: all $transition-fast;

  &:hover {
    box-shadow: $shadow-sm;
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 300px;
    color: $text-secondary;
    font-size: 14px;
  }

  &__container {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 60px;
    min-width: 600px;
  }

  &__level {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 40px;

    &--root {
      margin-bottom: 20px;
    }

    &--1 {
      width: 100%;
    }
  }

  &__branch {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  &__node {
    padding: 12px 20px;
    border-radius: $radius-md;
    font-size: 14px;
    text-align: center;
    min-width: 100px;
    max-width: 160px;
    word-break: break-word;
    transition: all $transition-fast;
    position: relative;

    &--root {
      background: linear-gradient(135deg, $primary-color, $primary-dark);
      color: #fff;
      font-weight: 600;
      font-size: 16px;
      padding: 16px 32px;
      min-width: 140px;
      box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
    }

    &--parent {
      background: $bg-card;
      border: 2px solid $primary-color;
      color: $text-primary;
      font-weight: 500;

      &.is-expandable {
        cursor: pointer;

        &:hover {
          background: rgba(59, 130, 246, 0.08);
          transform: translateY(-2px);
          box-shadow: $shadow-sm;
        }
      }
    }

    &--child {
      background: rgba(16, 185, 129, 0.08);
      border: 2px solid $success-color;
      color: $text-primary;
      font-weight: 500;

      &:hover {
        background: rgba(16, 185, 129, 0.12);
        transform: translateY(-2px);
      }
    }
  }

  &__node-text {
    display: block;
  }

  &__toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    margin-left: 6px;
    font-weight: bold;
    color: $primary-color;
    background: $border-lighter;
    border-radius: 50%;
    font-size: 12px;
    transition: all $transition-fast;
  }

  &__children {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    margin-top: 20px;
    justify-content: center;
  }

  &__sub-branch {
    display: flex;
    align-items: center;
  }
}
</style>
