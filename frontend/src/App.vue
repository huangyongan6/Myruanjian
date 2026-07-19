<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStudentStore } from '@/stores/student'
import { useChatStore } from '@/stores/chat'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const studentStore = useStudentStore()
const chatStore = useChatStore()

interface NavItem {
  path: string
  title: string
  icon: string
}

const navItems: NavItem[] = [
  { path: '/chat', title: '对话学习', icon: 'ChatDotRound' },
  { path: '/profile', title: '学习画像', icon: 'DataAnalysis' },
  { path: '/resources', title: '资源中心', icon: 'Files' },
  { path: '/path', title: '学习路径', icon: 'Promotion' },
  { path: '/dashboard', title: '学习仪表盘', icon: 'Odometer' }
]

const activePath = computed(() => route.path)
const currentTitle = computed(
  () => navItems.find((item) => item.path === route.path)?.title ?? '智能学习助手'
)
const currentSubtitle = computed(() => {
  const subtitles: Record<string, string> = {
    '/chat': '与 AI 助手对话，逐步构建你的学习画像，并触发个性化资源生成。',
    '/profile': '基于对话内容自动抽取的 6 维学习画像。可视化展示能力分布与学习偏好。',
    '/resources': '5 种个性化学习资源：课程讲解 / 思维导图 / 练习题库 / 拓展阅读 / 代码实操。',
    '/path': '基于你的学习画像与历史记录，AI 智能规划的学习步骤序列。',
    '/dashboard': '学习行为统计与效果评估。用户进行浏览 / 完成 / 答题后，本页实时刷新。'
  }
  return subtitles[route.path] ?? ''
})
const isWelcomePage = computed(() => route.name === 'Welcome')

function navigate(path: string): void {
  if (path !== route.path) {
    router.push(path)
  }
}

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？退出后将返回首页',
      '退出登录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    studentStore.logout()
    chatStore.logout()
    ElMessage.success('退出成功')
    router.push('/')
  } catch {
    ElMessage.info('已取消退出')
  }
}

async function handleSwitchAccount(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确定要切换账号吗？切换后将需要重新选择学生',
      '切换账号',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    studentStore.logout()
    chatStore.logout()
    await router.push('/chat')
    ElMessage.success('请创建或选择学生')
  } catch {
    ElMessage.info('已取消切换')
  }
}
</script>

<template>
  <el-container class="app-container">
    <template v-if="!isWelcomePage">
      <el-aside width="240px" class="app-aside">
        <div class="app-logo">
          <div class="app-logo__icon">
            <el-icon :size="20" color="#fff"><Reading /></el-icon>
          </div>
          <div class="app-logo__text">
            <span class="app-logo__title">智能学习助手</span>
            <span class="app-logo__subtitle">AI-Powered Learning</span>
          </div>
        </div>
        <el-menu
          :default-active="activePath"
          class="app-menu"
          background-color="transparent"
          text-color="#94A3B8"
          active-text-color="#fff"
          @select="navigate"
        >
          <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
            <div class="app-menu-item__icon-wrap">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
            </div>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
        <div class="app-aside__glow-right"></div>
      </el-aside>

      <el-container>
        <el-header class="app-header">
          <div class="app-header__left">
            <span class="app-header__title">{{ currentTitle }}</span>
            <span v-if="currentSubtitle" class="app-header__subtitle">{{ currentSubtitle }}</span>
          </div>
          <div class="app-header__right">
            <el-dropdown v-if="studentStore.currentStudent" trigger="click" class="app-header__dropdown">
              <span class="app-header__student">
                <el-avatar :size="36" class="app-header__avatar">
                  {{ studentStore.currentStudent.name.charAt(0) }}
                </el-avatar>
                <span>{{ studentStore.currentStudent.name }}</span>
                <el-icon :size="16" class="app-header__dropdown-arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item icon="SwitchButton" @click="handleSwitchAccount">
                    切换账号
                  </el-dropdown-item>
                  <li class="el-dropdown-menu__item el-dropdown-menu__item--divider" role="separator"></li>
                  <el-dropdown-item icon="Logout" class="app-header__dropdown-item--danger" @click="handleLogout">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-tag v-else type="warning" size="small">未选择学生</el-tag>
          </div>
        </el-header>

        <el-main class="app-main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </template>
    <template v-else>
      <el-main class="app-main app-main--full">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </template>
  </el-container>
</template>

<style scoped lang="scss">
.app-container {
  height: 100vh;
}

.app-aside {
  background: linear-gradient(180deg, #0a0a1a 0%, #0f0f23 50%, #0a0a1a 100%);
  color: #fff;
  overflow-x: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 250px;
    background: radial-gradient(ellipse at 50% 0%, rgba(0, 212, 255, 0.08) 0%, rgba(168, 85, 247, 0.04) 40%, transparent 70%);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    top: 60px;
    left: 0;
    width: 3px;
    height: calc(100% - 120px);
    background: linear-gradient(180deg, #00d4ff 0%, rgba(168, 85, 247, 0.5) 50%, transparent 100%);
    border-radius: 0 3px 3px 0;
    pointer-events: none;
  }

  &__glow-right {
    content: '';
    position: absolute;
    top: 0;
    right: -10px;
    width: 20px;
    height: 100%;
    background: linear-gradient(90deg, rgba(0, 212, 255, 0.05) 0%, transparent 100%);
    pointer-events: none;
  }
}

.app-logo {
  height: $header-height;
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: 0 $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: $radius-md;
    background: linear-gradient(135deg, #00d4ff, #a855f7);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
  }

  &__text {
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #00d4ff 0%, #a855f7 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    white-space: nowrap;
    letter-spacing: 2px;
  }

  &__subtitle {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 3px;
    text-transform: uppercase;
  }
}

.app-menu {
  border-right: none;
  padding-top: $spacing-md;

  :deep(.el-menu-item) {
    margin: 0 $spacing-md;
    border-radius: $radius-md;
    height: 44px;
    line-height: 44px;
    margin-bottom: 4px;
    transition: all $transition-normal;
    position: relative;

    &:hover {
      background: rgba(255, 255, 255, 0.04);
    }

    &.is-active {
      background: linear-gradient(135deg, rgba(0, 212, 255, 0.12) 0%, rgba(168, 85, 247, 0.08) 100%);
      border-left: 3px solid #00d4ff;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: radial-gradient(ellipse at 0% 50%, rgba(0, 212, 255, 0.15) 0%, transparent 70%);
        border-radius: $radius-md;
        pointer-events: none;
      }
    }
  }

  :deep(.el-menu-item__icon) {
    margin-right: $spacing-md;
    transition: color $transition-fast;
  }

  :deep(.el-menu-item.is-active .el-menu-item__icon) {
    color: #00d4ff;
    filter: drop-shadow(0 0 8px rgba(0, 212, 255, 0.5));
  }

  :deep(.el-menu-item.is-active) {
    color: #fff;

    .el-menu-item__text {
      background: linear-gradient(135deg, #00d4ff 0%, #a855f7 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

.app-header {
  background: $bg-card;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-lg;
  height: $header-height;
  box-shadow: $shadow-sm;
}

.app-header__left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.app-header__title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.01em;
}

.app-header__subtitle {
  font-size: 13px;
  color: $text-secondary;
  border-left: 1px solid $border-light;
  padding-left: $spacing-md;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.app-header__student {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: 14px;
  color: $text-regular;
  cursor: pointer;
  padding: 4px $spacing-sm;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: rgba(59, 130, 246, 0.06);
  }
}

.app-header__avatar {
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  font-weight: 600;
  color: #fff;
}

.app-header__dropdown-arrow {
  color: $text-secondary;
  transition: transform $transition-fast;

  .el-dropdown.is-active & {
    transform: rotate(180deg);
  }
}

.app-header__dropdown-item--danger {
  color: $danger-color;

  &:hover {
    background: rgba(239, 68, 68, 0.08);
  }
}

.app-main {
  background: linear-gradient(90deg, rgba(10, 10, 26, 0.1) 0%, $bg-page 10%);
  padding: 0;
  overflow-y: auto;

  &--full {
    padding: 0;
    overflow-y: auto;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
