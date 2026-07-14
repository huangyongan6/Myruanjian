# 开发环境

> 本文档汇总本项目所需的全部开发环境配置。所有成员在本机开发时必须使用一致的配置，避免出现「在我电脑上能跑」的问题。

---

## 一、数据库环境（MySQL）

| 项目 | 值 |
|------|---|
| 数据库类型 | MySQL 8.0 |
| 数据库名 | `Random` |
| 数据库地址 | `localhost` |
| 数据库端口 | `3306` |
| 用户名 | `root` |
| 密码 | `hya20050514` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 存储引擎 | `InnoDB` |

### 1.1 初始化

```bash
# 1. 启动 MySQL（Docker 方式）
docker run -d --name learngen-mysql \
  -e MYSQL_ROOT_PASSWORD=hya20050514 \
  -e MYSQL_DATABASE=Random \
  -p 3306:3306 \
  -v $(pwd)/data/mysql:/var/lib/mysql \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

# 2. 验证连接
mysql -h localhost -P 3306 -u root -phya20050514 -e "SELECT VERSION();"
```

### 1.2 后端 application.yml 引用

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/Random?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:hya20050514}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

> **安全提醒**：`hya20050514` 是开发环境密码，仅在本机配置。生产环境必须通过环境变量注入，**禁止**硬编码到代码仓库。

---

## 二、Redis 环境

| 项目 | 值 |
|------|---|
| Redis 地址 | `localhost` |
| Redis 端口 | `6379` |
| Redis 密码 | 无密码 |
| 数据库 | `0` |

### 2.1 启动

```bash
# Docker 方式
docker run -d --name learngen-redis \
  -p 6379:6379 \
  redis:7-alpine

# 验证
redis-cli -h localhost -p 6379 ping
# 返回 PONG
```

### 2.2 后端 application.yml 引用

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 5s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
```

---

## 三、后端环境（Spring Boot）

| 项目 | 版本 / 要求 | 说明 |
|------|-------------|------|
| JDK | 17（Temurin 推荐） | 项目使用 `eclipse-temurin:17-jre` 镜像 |
| Maven | 3.9+ | 构建工具 |
| Spring Boot | 3.2.x | 主框架 |
| MyBatis-Plus | 3.5.x | ORM |
| OkHttp | 4.12.0 | 讯飞 API 客户端 |
| Lombok | 最新稳定版 | 简化 POJO |
| IDE | IntelliJ IDEA 2023+ | 推荐 |

### 3.1 环境安装

#### JDK 17

```bash
# 推荐使用 SDKMAN（macOS/Linux）
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.10-tem

# Windows：手动下载 Temurin 17
# https://adoptium.net/temurin/releases/?version=17
# 设置 JAVA_HOME 环境变量
```

验证：

```bash
java -version
# openjdk version "17.0.10" 2024-01-16

javac -version
# javac 17.0.10
```

#### Maven 3.9+

```bash
sdk install maven 3.9.6

# 验证
mvn -v
# Apache Maven 3.9.6
# Java version: 17.0.10
```

### 3.2 IntelliJ IDEA 配置

1. **JDK**：`File → Project Structure → Project SDK` 选择 JDK 17
2. **Maven**：`File → Settings → Build, Execution, Deployment → Build Tools → Maven`
   - Maven home: `~/.sdkman/candidates/maven/current`
   - User settings file: `~/.m2/settings.xml`（可选，国内镜像）
3. **Lombok 插件**：`File → Settings → Plugins` 搜索并安装 Lombok
4. **编码**：`File → Settings → Editor → File Encodings`
   - Global Encoding: `UTF-8`
   - Project Encoding: `UTF-8`
   - Default encoding for properties: `UTF-8`

### 3.3 国内 Maven 镜像（settings.xml）

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云 Maven 仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### 3.4 启动与构建

```bash
cd backend

# 编译
mvn clean compile

# 启动开发服务器
mvn spring-boot:run

# 打包
mvn clean package -DskipTests

# 运行 jar
java -jar target/learngen-backend-1.0.0-SNAPSHOT.jar
```

### 3.5 环境变量（讯飞星火 APIPassword）

> Spark HTTP 鉴权为 Bearer Token 模式，只使用 **APIPassword**（单字段），不需要旧文档中的 api-key + api-secret。

**主备切换（§20.1）**：默认走 primary，secondary 可选。只要 `SPARK_API_PASSWORD_SECONDARY` 为空即降级为单 key 行为。

```bash
# macOS / Linux（写入 ~/.zshrc 或 ~/.bashrc）
export SPARK_API_PASSWORD_PRIMARY=your_api_password_here
export SPARK_API_PASSWORD_SECONDARY=
export DB_PASSWORD=hya20050514

# Windows PowerShell
$env:SPARK_API_PASSWORD_PRIMARY="your_api_password_here"
$env:SPARK_API_PASSWORD_SECONDARY=""
$env:DB_PASSWORD="hya20050514"

# Windows CMD
set SPARK_API_PASSWORD_PRIMARY=your_api_password_here
set SPARK_API_PASSWORD_SECONDARY=
set DB_PASSWORD=hya20050514
```

> **APIPassword 严禁提交到代码仓库**。如果误提交，需立即在讯飞控制台重置 Key。
> 如果在对话中粘贴过 Key，同样建议在控制台主动重置——对话日志可能被缓存。

---

## 四、前端环境（Vue 3）

| 项目 | 版本 / 要求 | 说明 |
|------|-------------|------|
| Node.js | 18.x 或 20.x LTS | 推荐 20.x |
| 包管理器 | npm 9+ / pnpm 8+ | 推荐 pnpm |
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 |
| IDE | VS Code | 推荐 |

### 4.1 环境安装

#### Node.js（推荐 nvm）

```bash
# macOS / Linux
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
nvm install 20
nvm use 20
nvm alias default 20

# Windows：使用 nvm-windows
# https://github.com/coreybutler/nvm-windows/releases
```

验证：

```bash
node -v
# v20.x.x

npm -v
# 10.x.x
```

#### pnpm（推荐）

```bash
npm install -g pnpm

# 验证
pnpm -v
# 8.x.x
```

### 4.2 VS Code 配置

#### 必装插件

| 插件 | 用途 |
|------|------|
| **Vue - Official**（Vue.volar） | Vue 3 + TypeScript 支持 |
| **ESLint** | 代码检查 |
| **Prettier** | 代码格式化 |
| **Stylelint** | 样式检查 |
| **Auto Rename Tag** | 自动重命名 HTML 标签 |
| **Path Intellisense** | 路径自动补全 |
| **Chinese Language Pack** | 中文语言包（可选） |

#### VS Code settings.json

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[vue]": {
    "editor.defaultFormatter": "Vue.volar"
  },
  "editor.tabSize": 2,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib"
}
```

### 4.3 国内 npm 镜像（提升安装速度）

```bash
# 设置淘宝镜像
npm config set registry https://registry.npmmirror.com

# 或使用 nrm 切换
npm install -g nrm
nrm use taobao

# pnpm
pnpm config set registry https://registry.npmmirror.com
```

### 4.4 启动与构建

```bash
cd frontend

# 安装依赖
npm install
# 或
pnpm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev
# 或
pnpm dev

# 类型检查 + 构建
npm run build
# 或
pnpm build

# 类型检查（不构建）
npm run type-check

# 代码检查
npm run lint
```

### 4.5 环境变量（`.env.development`）

```bash
# frontend/.env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws
```

```bash
# frontend/.env.production
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=/ws
```

> `.env.local` 为本机私有配置，**禁止**提交到仓库。在 `.gitignore` 中加入。

---

## 五、Docker 环境（推荐）

如需使用 Docker Compose 一键启动全部服务：

### 5.1 安装 Docker

- Windows / macOS：下载 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Linux：使用发行版包管理器（如 `apt install docker.io docker-compose`）

### 5.2 一键启动

```bash
# 根目录
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 停止
docker-compose down
```

### 5.3 docker-compose.yml 示例

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: learngen-mysql
    environment:
      MYSQL_ROOT_PASSWORD: hya20050514
      MYSQL_DATABASE: Random
    ports:
      - "3306:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
    command:
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: learngen-redis
    ports:
      - "6379:6379"

  backend:
    build: ./backend
    container_name: learngen-backend
    depends_on:
      - mysql
      - redis
    environment:
      DB_USER: root
      DB_PASSWORD: hya20050514
      SPARK_API_PASSWORD_PRIMARY: ${SPARK_API_PASSWORD_PRIMARY}
      SPARK_API_PASSWORD_SECONDARY: ${SPARK_API_PASSWORD_SECONDARY:-}
    ports:
      - "8080:8080"
```

---

## 六、端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| 后端 Spring Boot | 8080 | REST + WebSocket |
| 前端 Vite Dev | 5173 | 开发服务器 |
| 前端 Nginx（生产） | 80 / 443 | 部署时使用 |

> 启动前请确认端口未被占用：`netstat -ano | findstr :8080`（Windows）/ `lsof -i :8080`（macOS/Linux）

---

## 七、首次启动 Checklist

- [ ] JDK 17 安装并配置 `JAVA_HOME`
- [ ] Maven 3.9+ 安装并配置国内镜像
- [ ] Node.js 20 LTS 安装
- [ ] pnpm 安装（可选，推荐）
- [ ] VS Code + 必备插件安装
- [ ] IntelliJ IDEA + Lombok 插件安装
- [ ] MySQL 8.0 启动，`Random` 数据库创建成功
- [ ] Redis 启动，`ping` 返回 `PONG`
- [ ] 讯飞 API Key 已申请并配置到环境变量
- [ ] 后端 `mvn compile` 通过
- [ ] 后端 `mvn spring-boot:run` 启动成功，访问 `http://localhost:8080` 无 404
- [ ] 前端 `pnpm install` 完成
- [ ] 前端 `pnpm dev` 启动成功，访问 `http://localhost:5173` 看到页面
- [ ] 前后端联调：前端能成功调用后端 API

---

## 八、常见问题

### 8.1 后端启动报错「Communications link failure」

数据库未启动或端口不通。检查：

```bash
docker ps | grep mysql
mysql -h localhost -P 3306 -u root -phya20050514 -e "SHOW DATABASES;"
```

### 8.2 前端启动报错「EADDRINUSE: 5173」

5173 端口被占用。修改 `vite.config.ts` 的 `server.port`，或关闭占用进程。

### 8.3 Lombok 不生效（IDEA）

1. 确认 IDEA 已安装 Lombok 插件
2. `File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors` 勾选 `Enable annotation processing`
3. 重启 IDEA

### 8.4 前端 axios 请求跨域

后端 `CorsConfig` 已配置允许 `http://localhost:5173`。如仍跨域，检查：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### 8.5 讯飞 API 调用 401 / 403

APIPassword（Bearer Token）配置错误或已过期。在讯飞控制台重新生成后更新
`SPARK_API_PASSWORD_PRIMARY` 环境变量（如有二级备 Key，同步更新 `SPARK_API_PASSWORD_SECONDARY`），然后重启后端。

如果是主备切换导致 secondary 误用，检查启动日志中的 `SparkRouter` 初始化信息：
`secondary 已配置(主备切换启用)` 或 `降级为单 key`。