<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'

interface Props {
  code: string
  language?: string
}
const props = withDefaults(defineProps<Props>(), { language: 'python' })

const containerRef = ref<HTMLDivElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

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

function init(): void {
  if (!containerRef.value) return
  editor = monaco.editor.create(containerRef.value, {
    value: props.code,
    language: detectLanguage(props.language ?? 'python'),
    theme: 'vs',
    readOnly: true,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 13,
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    folding: true,
    wordWrap: 'on'
  })
}

onMounted(init)

watch(
  () => [props.code, props.language],
  ([code, lang]) => {
    if (!editor) return
    const model = editor.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, detectLanguage(String(lang)))
      model.setValue(String(code))
    }
  }
)

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})
</script>

<template>
  <div class="code-viewer">
    <div ref="containerRef" class="code-viewer__container" />
  </div>
</template>

<style scoped lang="scss">
.code-viewer {
  width: 100%;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  overflow: hidden;

  &__container {
    width: 100%;
    height: 400px;
  }
}
</style>
