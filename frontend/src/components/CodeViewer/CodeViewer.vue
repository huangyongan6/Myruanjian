<script setup lang="ts">
import { computed } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

interface Props {
  code: string
  language?: string
}
const props = withDefaults(defineProps<Props>(), { language: 'python' })

function detectLanguage(lang: string): string {
  const normalized = lang.toLowerCase()
  const map: Record<string, string> = {
    py: 'python',
    js: 'javascript',
    ts: 'typescript',
    java: 'java',
    cpp: 'cpp',
    c: 'c',
    sql: 'sql',
    md: 'markdown',
    json: 'json'
  }
  return map[normalized] ?? normalized
}

const highlightedCode = computed(() => {
  const lang = detectLanguage(props.language ?? 'python')
  if (lang && hljs.getLanguage(lang)) {
    try {
      return hljs.highlight(props.code, { language: lang, ignoreIllegals: true }).value
    } catch {
      // 忽略
    }
  }
  return hljs.highlightAuto(props.code).value
})
</script>

<template>
  <div class="code-viewer">
    <div class="code-viewer__header">
      <span class="code-viewer__lang">{{ detectLanguage(language ?? 'python') }}</span>
    </div>
    <pre class="code-viewer__pre"><code class="code-viewer__code" v-html="highlightedCode" /></pre>
  </div>
</template>

<style scss scoped>
.code-viewer {
  width: 100%;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  overflow: hidden;
  transition: all $transition-fast;

  &:hover {
    box-shadow: $shadow-sm;
    border-color: $primary-color;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm $spacing-md;
    background: $bg-card;
    border-bottom: 1px solid $border-light;
  }

  &__lang {
    font-size: 12px;
    font-weight: 600;
    color: $text-secondary;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__pre {
    margin: 0;
    padding: $spacing-lg;
    background: $bg-card;
    overflow-x: auto;
    max-height: 400px;
    overflow-y: auto;
  }

  &__code {
    font-size: 13px;
    font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
    line-height: 1.6;
    color: $text-primary;
    display: block;
    white-space: pre;
  }
}

:deep(.hljs) {
  background: $bg-card;
}
</style>