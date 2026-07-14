<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStudentStore } from '@/stores/student'

const route = useRoute()
const router = useRouter()
const studentStore = useStudentStore()

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
  { path: '/tutor', title: '智能辅导', icon: 'Service' },
  { path: '/dashboard', title: '学习仪表盘', icon: 'Odometer' }
]

const activePath = computed(() => route.path)
const currentTitle = computed(
  () => navItems.find((item) => item.path === route.path)?.title ?? '智能学习助手'
)

function navigate(path: string): void {
  if (path !== route.path) {
    router.push(path)
  }
}
</script>

<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="app-logo">
        <el-icon :size="22" color="#fff"><Reading /></el-icon>
        <span class="app-logo__title">智能学习助手</span>
      </div>
      <el-menu
        :default-active="activePath"
        class="app-menu"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="navigate"
      >
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <span class="app-header__title">{{ currentTitle }}</span>
        <div class="app-header__right">
          <span v-if="studentStore.currentStudent" class="app-header__student">
            当前学生：{{ studentStore.currentStudent.name }} (ID: {{ studentStore.currentStudent.id }})
          </span>
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
  </el-container>
</template>

<style scoped lang="scss">
.app-container {
  height: 100vh;
}

.app-aside {
  background-color: #1f2d3d;
  color: #fff;
  overflow-x: hidden;
}

.app-logo {
  height: $header-height;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: 0 $spacing-md;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
  }
}

.app-menu {
  border-right: none;
}

.app-header {
  background: $bg-card;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-lg;
  height: $header-height;

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  &__student {
    font-size: 13px;
    color: $text-secondary;
  }
}

.app-main {
  background-color: $bg-page;
  padding: 0;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
