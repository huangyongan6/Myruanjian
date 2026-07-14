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
  line-height: 1.7;
  color: $text-primary;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 0.8em 0 0.4em;
    font-weight: 600;
    line-height: 1.3;
  }
  :deep(h1) { font-size: 1.6em; }
  :deep(h2) { font-size: 1.35em; }
  :deep(h3) { font-size: 1.15em; }
  :deep(p) { margin: 0.4em 0; }
  :deep(ul),
  :deep(ol) { margin: 0.4em 0; padding-left: 1.5em; }
  :deep(li) { margin: 0.2em 0; }
  :deep(a) { color: $primary-color; text-decoration: none; }
  :deep(a):hover { text-decoration: underline; }
  :deep(blockquote) {
    margin: 0.5em 0;
    padding: 0.2em 0.8em;
    border-left: 3px solid $primary-color;
    color: $text-regular;
    background: $border-extra-light;
  }
  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 0.6em 0;
  }
  :deep(th),
  :deep(td) {
    border: 1px solid $border-light;
    padding: 6px 10px;
  }
  :deep(th) { background: $border-extra-light; }
  :deep(code) {
    background: rgba(27, 31, 35, 0.05);
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 0.9em;
    font-family: 'SFMono-Regular', Consolas, monospace;
  }
  :deep(pre) {
    margin: 0.6em 0;
    padding: $spacing-md;
    background: #f6f8fa;
    border-radius: $radius-sm;
    overflow-x: auto;
  }
  :deep(pre code) {
    background: transparent;
    padding: 0;
    font-size: 13px;
  }
  :deep(.hljs) {
    background: #f6f8fa;
  }

  // KaTeX 渲染出的数学公式 — 仅做版式修饰，字体沿用 KaTeX 默认（衬线/宋体观感）
  :deep(.katex-display) {
    margin: 0.8em 0;
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
}
</style>
