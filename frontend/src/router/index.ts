import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置。
 *
 * <p>使用 **history 模式**（createWebHistory），URL 形如 /chat、/profile。
 * 部署时需配置 Nginx try_files $uri $uri/ /index.html。
 *
 * <p>所有路由懒加载；name 必填便于编程式导航；路径使用 kebab-case（CLAUDE.md §4.2）。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat'
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/pages/ChatPage/ChatPage.vue'),
      meta: { title: '对话学习' }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/pages/ProfilePage/ProfilePage.vue'),
      meta: { title: '学习画像' }
    },
    {
      path: '/resources',
      name: 'ResourceCenter',
      component: () => import('@/pages/ResourceCenter/ResourceCenter.vue'),
      meta: { title: '资源中心' }
    },
    {
      path: '/path',
      name: 'LearningPath',
      component: () => import('@/pages/LearningPath/LearningPath.vue'),
      meta: { title: '学习路径' }
    },
    {
      path: '/tutor',
      name: 'Tutor',
      component: () => import('@/pages/TutorPage/TutorPage.vue'),
      meta: { title: '智能辅导' }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/pages/Dashboard/Dashboard.vue'),
      meta: { title: '学习仪表盘' }
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  const title = (to.meta?.title as string | undefined) ?? '智能学习助手'
  document.title = `${title} - 智能学习助手`
})

export default router
