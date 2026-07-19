<script setup lang="ts">
import { computed, shallowRef } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import MarkdownItKatex from 'markdown-it-katex'
import 'highlight.js/styles/github.css'
import 'katex/dist/katex.min.css'

interface Props {
  content: string
}
const props = defineProps<Props>()

const md = shallowRef<MarkdownIt>(
  new MarkdownIt({
    html: false,
    linkify: true,
    breaks: true,
    highlight(str: string, lang: string): string {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
        } catch {
          // 忽略
        }
      }
      const escaped = md.value.utils.escapeHtml(str)
      return `<pre class="hljs"><code>${escaped}</code></pre>`
    }
  })
)

// 挂载 KaTeX 插件，启用 $$...$$ / $...$ / \[..\] / \(..\) 等 LaTeX 公式渲染
md.value.use(MarkdownItKatex, {
  throwOnError: false,
  // 允许识别 \$ 转义后的美元号
  strict: false
})

const html = computed(() => {
  if (!props.content) return ''
  return md.value.render(props.content)
})
</script>

<template>
  <div class="markdown-renderer" v-html="html" />
</template>

<style scoped lang="scss">
.markdown-renderer {
  font-size: 14px;
  line-height: 1.8;
  color: $text-primary;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 1em 0 0.5em;
    font-weight: 600;
    line-height: 1.3;
    letter-spacing: -0.02em;
  }

  :deep(h1) {
    font-size: 1.8em;
    padding-bottom: 0.3em;
    border-bottom: 2px solid $border-light;
    color: $text-primary;
  }

  :deep(h2) {
    font-size: 1.4em;
    padding-bottom: 0.2em;
    border-bottom: 1px solid $border-lighter;
    color: $text-primary;
  }

  :deep(h3) {
    font-size: 1.2em;
    color: $text-primary;
  }

  :deep(h4) {
    font-size: 1.05em;
    color: $text-primary;
  }

  :deep(p) {
    margin: 0.5em 0;
    color: $text-regular;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0.5em 0;
    padding-left: 1.8em;
    color: $text-regular;
  }

  :deep(li) {
    margin: 0.3em 0;
    line-height: 1.7;
  }

  :deep(a) {
    color: $primary-color;
    text-decoration: none;
    font-weight: 500;
    transition: all $transition-fast;

    &:hover {
      text-decoration: underline;
      opacity: 0.85;
    }
  }

  :deep(blockquote) {
    margin: 0.8em 0;
    padding: $spacing-md $spacing-lg;
    border-left: 4px solid $primary-color;
    color: $text-secondary;
    background: rgba(59, 130, 246, 0.04);
    border-radius: 0 $radius-md $radius-md 0;
    font-style: italic;
    font-size: 14px;
    line-height: 1.7;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 0.8em 0;
    background: $bg-card;
    border-radius: $radius-md;
    overflow: hidden;
    border: 1px solid $border-light;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid $border-lighter;
    padding: $spacing-sm $spacing-md;
    text-align: left;
    font-size: 13px;
  }

  :deep(th) {
    background: $border-lighter;
    font-weight: 600;
    color: $text-primary;
  }

  :deep(code) {
    background: rgba(59, 130, 246, 0.08);
    padding: 3px 8px;
    border-radius: $radius-sm;
    font-size: 0.88em;
    font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
    color: $primary-color;
    font-weight: 500;
  }

  :deep(pre) {
    margin: 0.8em 0;
    padding: $spacing-lg;
    background: $bg-card;
    border-radius: $radius-md;
    overflow-x: auto;
    border: 1px solid $border-light;
    box-shadow: $shadow-sm;
  }

  :deep(pre code) {
    background: transparent;
    padding: 0;
    font-size: 13px;
    color: $text-primary;
    font-weight: 400;
    line-height: 1.6;
  }

  :deep(.hljs) {
    background: $bg-card;
  }

  :deep(.katex-display) {
    margin: 1em 0;
    overflow-x: auto;
    overflow-y: hidden;
  }

  :deep(.katex) {
    font-size: 1.05em;
  }

  :deep(.katex-display > .katex) {
    display: inline-block;
    text-align: center;
  }

  :deep(hr) {
    margin: 1.5em 0;
    border: none;
    height: 1px;
    background: $border-light;
  }

  :deep(strong) {
    color: $text-primary;
    font-weight: 600;
  }

  :deep(em) {
    color: $text-regular;
    font-style: italic;
  }
}
</style>
