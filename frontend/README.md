# LearnGen Frontend

基于大模型的个性化资源生成与学习多智能体系统 - 前端工程（Vue 3 + TypeScript + Vite）。

完整规范见 [doc/frontend-说明.md](../doc/frontend-说明.md) 与 [doc/CLAUDE.md](../doc/CLAUDE.md)。

## 环境

- Node.js 20 LTS
- 包管理器：npm / pnpm（推荐 pnpm）

## 启动

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 类型检查
npm run type-check

# 生产构建
npm run build
```

## 后端联调

默认通过 Vite 代理 `/api` 与 `/ws` 到 `http://localhost:8080`（参见 `vite.config.ts`）。
修改后端地址：编辑 `.env.development` 中的 `VITE_API_PROXY_TARGET`。

## 目录结构

```
src/
├── pages/        页面级组件（6 个）
├── components/   可复用组件（10 个）
├── composables/  组合式函数
├── stores/       Pinia 状态
├── services/     Axios API 封装
├── router/       Vue Router 配置
├── types/        TypeScript 类型
└── utils/        工具函数
```

## 路由

使用 **history 模式**（`createWebHistory`），后端部署需配置 Nginx `try_files` 回退到 `index.html`。
