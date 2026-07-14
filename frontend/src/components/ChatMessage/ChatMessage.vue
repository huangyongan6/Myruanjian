<script setup lang="ts">
import MarkdownRenderer from '@/components/MarkdownRenderer/MarkdownRenderer.vue'
import type { ChatMessage } from '@/types/chat'

interface Props {
  message: ChatMessage
  streaming?: boolean
}

const props = withDefaults(defineProps<Props>(), { streaming: false })

const isUser = (): boolean => props.message.role === 'user'
const isAssistant = (): boolean => props.message.role === 'assistant'

function getAvatar(): string {
  if (isUser()) return '👤'
  if (isAssistant()) return '🤖'
  return '💡'
}

function getName(): string {
  if (isUser()) return '我'
  if (isAssistant()) return 'AI 助手'
  return '系统'
}
</script>

<template>
  <div class="chat-message" :class="{ 'chat-message--user': isUser(), 'chat-message--assistant': isAssistant() }">
    <div class="chat-message__avatar">{{ getAvatar() }}</div>
    <div class="chat-message__body">
      <div class="chat-message__meta">
        <span class="chat-message__name">{{ getName() }}</span>
        <el-tag v-if="message.agentType" type="info" size="small" effect="plain" round>
          {{ message.agentType }}
        </el-tag>
        <span v-if="streaming" class="chat-message__streaming">
          <span class="dot-flashing" />
        </span>
      </div>
      <div class="chat-message__content">
        <MarkdownRenderer v-if="!isUser() && message.content" :content="message.content" />
        <template v-else>{{ message.content }}</template>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-message {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  &--user {
    flex-direction: row-reverse;
    .chat-message__body {
      align-items: flex-end;
    }
    .chat-message__meta {
      justify-content: flex-end;
    }
    .chat-message__content {
      background: $primary-color;
      color: #fff;
      border-radius: $radius-md $radius-md 4px $radius-md;
    }
  }
  &--assistant {
    .chat-message__content {
      background: $bg-card;
      border: 1px solid $border-lighter;
      border-radius: 4px $radius-md $radius-md $radius-md;
    }
  }
  &__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: $border-lighter;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }
  &__body {
    display: flex;
    flex-direction: column;
    max-width: 75%;
    gap: 4px;
  }
  &__meta {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 12px;
    color: $text-secondary;
  }
  &__name {
    font-weight: 500;
  }
  &__content {
    padding: $spacing-md;
    line-height: 1.6;
    word-break: break-word;
  }
  &__streaming {
    display: inline-flex;
    align-items: center;
  }
}

.dot-flashing {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $primary-color;
  margin: 0 2px;
  animation: dot-flashing 1.2s infinite ease-in-out;
}
@keyframes dot-flashing {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}
</style>
