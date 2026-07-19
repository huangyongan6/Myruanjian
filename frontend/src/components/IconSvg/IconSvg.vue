<script setup lang="ts">
defineProps<{
  name: string
  size?: number | string
  spin?: boolean
}>()
</script>

<template>
  <svg
    class="icon-svg"
    :class="{ 'icon-svg--spin': spin }"
    :width="size || 16"
    :height="size || 16"
    viewBox="0 0 24 24"
    fill="currentColor"
    xmlns="http://www.w3.org/2000/svg"
  >
    <!-- 用户头像 👤 -->
    <template v-if="name === 'user'">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
    </template>

    <!-- AI 机器人 🤖 -->
    <template v-else-if="name === 'robot'">
      <rect x="4" y="8" width="16" height="12" rx="2" />
      <circle cx="9" cy="13" r="1.5" fill="currentColor" />
      <circle cx="15" cy="13" r="1.5" fill="currentColor" />
      <path d="M9 17h6" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M12 4v4" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <circle cx="12" cy="3" r="1.5" />
    </template>

    <!-- 灯泡 💡 -->
    <template v-else-if="name === 'idea'">
      <path d="M9 21h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <path d="M12 17v4" stroke="currentColor" stroke-width="2" />
      <path d="M8 3h8l-1 6a4 4 0 01-6 0L8 3z" />
      <path d="M8 9c0-2 1.5-4 4-4s4 2 4 4" fill="none" stroke="currentColor" stroke-width="1.5" />
    </template>

    <!-- 书本 📚 -->
    <template v-else-if="name === 'book'">
      <path d="M4 19.5A2.5 2.5 0 016.5 17H20" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M8 7h8M8 11h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </template>

    <!-- 图表 📊 -->
    <template v-else-if="name === 'chart'">
      <rect x="3" y="12" width="4" height="9" rx="1" fill="currentColor" opacity="0.6" />
      <rect x="10" y="8" width="4" height="13" rx="1" fill="currentColor" opacity="0.8" />
      <rect x="17" y="3" width="4" height="18" rx="1" fill="currentColor" />
    </template>

    <!-- 发射/开始 🚀 -->
    <template v-else-if="name === 'rocket'">
      <path d="M12 2c2 3 4 6 4 10a4 4 0 11-8 0c0-4 2-7 4-10z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M12 12l3-3M12 12l-3-3M12 12v6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </template>

    <!-- 文件夹 📂 -->
    <template v-else-if="name === 'folder'">
      <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z" stroke="currentColor" stroke-width="1.5" fill="none" />
    </template>

    <!-- 时钟 🕒 -->
    <template v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M12 6v6l4 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
    </template>

    <!-- 标记 📘 -->
    <template v-else-if="name === 'tag'">
      <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <circle cx="7.5" cy="7.5" r="1.5" fill="currentColor" />
    </template>

    <!-- 位置 📍 -->
    <template v-else-if="name === 'location'">
      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <circle cx="12" cy="9" r="2.5" stroke="currentColor" stroke-width="1.5" fill="none" />
    </template>

    <!-- 刷新 🔄 -->
    <template v-else-if="name === 'refresh'">
      <path d="M23 4v6h-6M1 20v-6h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
      <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
    </template>

    <!-- 手势 👋 -->
    <template v-else-if="name === 'wave'">
      <path d="M8 14s1.5 2 4 2 4-2 4-2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
      <path d="M4 16V8a2 2 0 012-2h1a2 2 0 012 2v1M8 6V4a2 2 0 012-2h0a2 2 0 012 2v5M12 4V2a2 2 0 012-2h0a2 2 0 012 2v7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
    </template>

    <!-- 罗盘/导航 🧭 -->
    <template v-else-if="name === 'compass'">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" fill="none" />
      <polygon points="12,5 15,12 12,19 9,12" fill="currentColor" opacity="0.6" />
      <polygon points="12,5 15,12 9,12" fill="currentColor" />
      <circle cx="12" cy="12" r="1.5" fill="currentColor" />
    </template>

    <!-- 目标 🎯 -->
    <template v-else-if="name === 'target'">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" fill="none" />
      <circle cx="12" cy="12" r="6" stroke="currentColor" stroke-width="1.5" fill="none" />
      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5" fill="none" />
      <circle cx="12" cy="12" r="1" fill="currentColor" />
    </template>

    <!-- 复选标记 📋 -->
    <template v-else-if="name === 'clipboard'">
      <rect x="6" y="3" width="12" height="18" rx="2" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M9 1h6M9 7h6M9 11h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      <path d="M9 14l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" fill="none" />
    </template>

    <!-- 大脑 🧠 -->
    <template v-else-if="name === 'brain'">
      <path d="M12 4a4 4 0 00-4 4c0 1.5.8 2.8 2 3.5-.5.5-.8 1.2-.8 2v2c0 1.5 1 3 3 3h.5c.5 1.5 1.5 3 3 3h.5c2 0 3-1.5 3-3v-2c0-.8-.3-1.5-.8-2 1.2-.7 2-2 2-3.5a4 4 0 00-4-4" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M12 8v8M8 12h8" stroke="currentColor" stroke-width="1" opacity="0.5" />
    </template>

    <!-- 警告 ⚠️ -->
    <template v-else-if="name === 'warning'">
      <path d="M12 2L2 21h20L12 2z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M12 9v4M12 16v1" stroke="currentColor" stroke-width="2" stroke-linecap="round" fill="none" />
    </template>

    <!-- 包裹 📦 -->
    <template v-else-if="name === 'box'">
      <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
    </template>

    <!-- 对话 💬 -->
    <template v-else-if="name === 'chat'">
      <path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z" stroke="currentColor" stroke-width="1.5" fill="none" />
    </template>

    <!-- 垃圾箱 🗑️ -->
    <template v-else-if="name === 'trash'">
      <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
    </template>

    <!-- 发送 ✈️ -->
    <template v-else-if="name === 'send'">
      <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
    </template>

    <!-- 默认问号 ❓ -->
    <template v-else>
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" fill="none" />
      <path d="M9 9a3 3 0 115.83 1c0 2-3 3-3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
      <circle cx="12" cy="17" r="0.5" fill="currentColor" />
    </template>
  </svg>
</template>

<style scoped lang="scss">
.icon-svg {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;

  &--spin {
    animation: spin 1.2s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
