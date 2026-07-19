<script setup lang="ts">
import MarkdownRenderer from '@/components/MarkdownRenderer/MarkdownRenderer.vue'
import IconSvg from '@/components/IconSvg/IconSvg.vue'
import type { ChatMessage } from '@/types/chat'

interface Props {
  message: ChatMessage
  streaming?: boolean
}

const props = withDefaults(defineProps<Props>(), { streaming: false })

const isUser = (): boolean => props.message.role === 'user'
const isAssistant = (): boolean => props.message.role === 'assistant'

function getAvatarIcon(): string {
  if (isUser()) return 'user'
  if (isAssistant()) return 'robot'
  return 'idea'
}

function getName(): string {
  if (isUser()) return '我'
  if (isAssistant()) return 'AI 助手'
  return '系统'
}
</script>

<template>
  <div class="chat-message" :class="{ 'chat-message--user': isUser(), 'chat-message--assistant': isAssistant() }">
    <div class="chat-message__avatar">
      <IconSvg :name="getAvatarIcon()" :size="20" />
    </div>
    <div class="chat-message__body">
      <div class="chat-message__meta">
        <span class="chat-message__name">{{ getName() }}</span>
        <el-tag v-if="message.agentType" type="info" size="small" effect="plain" round>
          {{ message.agentType }}
        </el-tag>
        <span v-if="streaming" class="chat-message__streaming">
          <span class="dot-flashing" />
          <span class="dot-flashing" />
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
  padding: $spacing-lg $spacing-lg;
  transition: all $transition-fast;

  &:hover {
    background: rgba(59, 130, 246, 0.02);
  }

  &--user {
    flex-direction: row-reverse;

    .chat-message__body {
      align-items: flex-end;
    }

    .chat-message__meta {
      justify-content: flex-end;
    }

    .chat-message__content {
      background: linear-gradient(135deg, $primary-color, $primary-dark);
      color: #fff;
      border-radius: $radius-lg $radius-lg $radius-sm $radius-lg;
      box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
    }

    .chat-message__name {
      color: $text-primary;
    }
  }

  &--assistant {
    .chat-message__content {
      background: $bg-card;
      border: 1px solid $border-light;
      border-radius: $radius-sm $radius-lg $radius-lg $radius-lg;
      box-shadow: $shadow-card;
    }

    .chat-message__name {
      color: $primary-color;
    }
  }

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: all $transition-fast;

    &:hover {
      transform: scale(1.05);
    }

    .chat-message--user & {
      background: linear-gradient(135deg, $primary-color, $primary-dark);
      color: #fff;
    }

    .chat-message--assistant & {
      background: $border-lighter;
      color: $primary-color;
    }
  }

  &__body {
    display: flex;
    flex-direction: column;
    max-width: 75%;
    gap: $spacing-xs;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 12px;
    color: $text-secondary;

    :deep(.el-tag) {
      font-size: 10px;
      padding: 2px 8px;
    }
  }

  &__name {
    font-weight: 600;
    font-size: 13px;
  }

  &__content {
    padding: $spacing-md $spacing-lg;
    line-height: 1.7;
    word-break: break-word;
    font-size: 14px;
    transition: all $transition-fast;
  }

  &__streaming {
    display: inline-flex;
    align-items: center;
    gap: 3px;
  }
}

.dot-flashing {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $primary-color;
  margin: 0 1.5px;
  animation: dot-flashing 1.2s infinite ease-in-out;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes dot-flashing {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.9); }
  40% { opacity: 1; transform: scale(1.1); }
}
</style>
