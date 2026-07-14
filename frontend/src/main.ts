import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 引入 Element Plus 全局样式与图标
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局样式
import '@/styles/global.scss'

/**
 * 应用入口。
 *
 * <p>职责：创建 Vue 应用、注册 Pinia、注册 Router、注册 Element Plus、挂载根组件。
 * 组件自动按需导入由 unplugin-vue-components 完成（参见 vite.config.ts）。
 */
const app = createApp(App)

// 注册所有 Element Plus 图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.mount('#app')
