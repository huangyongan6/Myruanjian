<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const visible = ref(false)

onMounted(() => {
  setTimeout(() => {
    visible.value = true
  }, 100)
})

function startLearning(): void {
  visible.value = false
  setTimeout(() => {
    router.push({ name: 'Chat' })
  }, 500)
}
</script>

<template>
  <div class="welcome">
    <!-- 背景装饰 -->
    <div class="welcome__bg">
      <div class="welcome__circle welcome__circle--1"></div>
      <div class="welcome__circle welcome__circle--2"></div>
      <div class="welcome__circle welcome__circle--3"></div>
      <div class="welcome__particles">
        <span v-for="i in 20" :key="i" class="welcome__particle" :style="{ '--i': i }"></span>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="welcome__content" :class="{ 'welcome__content--visible': visible }">
      <div class="welcome__icon">🤖</div>
      <h1 class="welcome__title">智能学习助手</h1>
      <p class="welcome__subtitle">基于 AI 的个性化机器学习学习平台</p>

      <div class="welcome__features">
        <div class="welcome__feature" :class="{ 'welcome__feature--animate': visible }" style="--delay: 0.2s">
          <div class="welcome__feature-icon">📚</div>
          <div class="welcome__feature-text">
            <h3>个性化学习路径</h3>
            <p>AI 根据你的情况定制专属计划</p>
          </div>
        </div>
        <div class="welcome__feature" :class="{ 'welcome__feature--animate': visible }" style="--delay: 0.4s">
          <div class="welcome__feature-icon">💡</div>
          <div class="welcome__feature-text">
            <h3>智能资源推荐</h3>
            <p>精准匹配你当前的学习需求</p>
          </div>
        </div>
        <div class="welcome__feature" :class="{ 'welcome__feature--animate': visible }" style="--delay: 0.6s">
          <div class="welcome__feature-icon">📊</div>
          <div class="welcome__feature-text">
            <h3>实时学习分析</h3>
            <p>追踪你的学习进度与效果</p>
          </div>
        </div>
      </div>

      <button class="welcome__btn" @click="startLearning">
        <span class="welcome__btn-text">开启学习之旅</span>
        <span class="welcome__btn-icon">🚀</span>
      </button>

      <div class="welcome__tech">
        <span class="welcome__tech-tag">机器学习</span>
        <span class="welcome__tech-tag">AI 生成</span>
        <span class="welcome__tech-tag">个性化</span>
      </div>
    </div>

    <!-- 底部装饰 -->
    <div class="welcome__footer">
      <p>让 AI 帮助你更好地学习机器学习</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.welcome {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  // 背景装饰
  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    overflow: hidden;
  }

  &__circle {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.3;

    &--1 {
      width: 500px;
      height: 500px;
      background: #667eea;
      top: -200px;
      left: -100px;
      animation: float 8s ease-in-out infinite;
    }

    &--2 {
      width: 400px;
      height: 400px;
      background: #764ba2;
      bottom: -150px;
      right: -100px;
      animation: float 10s ease-in-out infinite reverse;
    }

    &--3 {
      width: 300px;
      height: 300px;
      background: #f093fb;
      top: 50%;
      right: 20%;
      animation: float 12s ease-in-out infinite;
    }
  }

  &__particles {
    position: absolute;
    inset: 0;
  }

  &__particle {
    position: absolute;
    width: 4px;
    height: 4px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 50%;
    left: calc(var(--i) * 5%);
    top: calc(20% + 60% * (var(--i) / 20));
    animation: twinkle 2s ease-in-out infinite;
    animation-delay: calc(var(--i) * 0.1s);

    @keyframes twinkle {
      0%, 100% { opacity: 0.3; transform: scale(1); }
      50% { opacity: 1; transform: scale(1.5); }
    }
  }

  @keyframes float {
    0%, 100% { transform: translateY(0) rotate(0deg); }
    50% { transform: translateY(-30px) rotate(5deg); }
  }

  // 主内容
  &__content {
    position: relative;
    z-index: 1;
    text-align: center;
    padding: 40px;
    opacity: 0;
    transform: translateY(30px);
    transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);

    &--visible {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &__icon {
    font-size: 80px;
    margin-bottom: 24px;
    animation: bounce 2s ease-in-out infinite;
    filter: drop-shadow(0 0 20px rgba(102, 126, 234, 0.5));
  }

  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-10px); }
  }

  &__title {
    font-size: 56px;
    font-weight: 800;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 16px;
    text-shadow: 0 0 40px rgba(102, 126, 234, 0.3);
  }

  &__subtitle {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.7);
    margin: 0 0 48px;
    letter-spacing: 2px;
  }

  // 特性卡片
  &__features {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-bottom: 48px;
  }

  &__feature {
    display: flex;
    align-items: center;
    gap: 20px;
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    padding: 20px 24px;
    text-align: left;
    opacity: 0;
    transform: translateX(-30px);
    transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    transition-delay: var(--delay);

    &--animate {
      opacity: 1;
      transform: translateX(0);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(102, 126, 234, 0.5);
      transform: translateX(10px);
    }
  }

  &__feature-icon {
    font-size: 36px;
    flex-shrink: 0;
  }

  &__feature-text {
    h3 {
      margin: 0 0 4px;
      font-size: 18px;
      font-weight: 600;
      color: #fff;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.6);
    }
  }

  // 按钮
  &__btn {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 12px;
    padding: 18px 48px;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 50px;
    cursor: pointer;
    overflow: hidden;
    transition: all 0.3s ease;
    box-shadow: 0 10px 40px rgba(102, 126, 234, 0.4);

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 15px 50px rgba(102, 126, 234, 0.5);

      &::before {
        opacity: 1;
      }

      .welcome__btn-icon {
        transform: translateX(5px);
      }
    }

    &:active {
      transform: translateY(0);
    }
  }

  &__btn-text,
  &__btn-icon {
    position: relative;
    z-index: 1;
  }

  &__btn-icon {
    font-size: 20px;
    transition: transform 0.3s ease;
  }

  // 技术标签
  &__tech {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-top: 32px;
  }

  &__tech-tag {
    padding: 6px 16px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
    background: rgba(255, 255, 255, 0.1);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  // 底部
  &__footer {
    position: absolute;
    bottom: 40px;
    text-align: center;

    p {
      margin: 0;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.4);
    }
  }
}
</style>
