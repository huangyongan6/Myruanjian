# CLAUDE.md

> Claude Code 项目规范文件，修改代码时必须遵守。

---

## 一、项目介绍

**项目名称**：基于大模型的个性化资源生成与学习多智能体系统

**赛题背景**：第26届软件杯 A3 赛题，出题企业为科大讯飞股份有限公司。目标是借助大模型技术，构建高等教育个性化学习资源体系，以**机器学习**课程为切入点，实现个性化资源的自动化生成。

**团队分工**：3 人 —— 后端（Java）+ 前端（Vue 3）+ 文档&视频

**开发周期**：Day1-3 MVP 开发 → Day4-7 打磨提交

**核心功能**：
1. 对话式学习画像自主构建（≥6 维度）—— 必做
2. 多智能体协同资源生成（≥5 种资源类型）—— 必做
3. 个性化学习路径规划 + 资源推送 —— 必做
4. 智能辅导（多模态答疑）—— 加分
5. 学习效果评估（动态调整）—— 加分

**AI 模型**：讯飞星火 Spark（HTTP SSE 流式调用）

---

## 二、技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.x | 主框架 |
| JDK | 17 | Java 版本 |
| MyBatis-Plus | 3.5.x | ORM |
| MySQL | 8.0 | 数据库 |
| Spring WebSocket + STOMP | — | 实时通信（对话流式输出） |
| OkHttp | 4.x | HTTP 客户端（调用讯飞 API SSE 流式） |
| Maven | 3.9+ | 构建工具 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型支持 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由 |
| Axios | 1.x | HTTP 请求 |
| ECharts | 5.x | 图表（雷达图 + 统计图） |
| markmap | — | 思维导图渲染 |
| Monaco Editor | — | 代码展示（只读） |
| markdown-it | — | Markdown 渲染 |

### AI 服务

| 技术 | 说明 |
|------|------|
| 讯飞星火 Spark | 主力大模型 |
| HTTP SSE 流式接口 | 调用方式 |

---

## 三、开发规范

### 3.1 核心原则

> **一次只做一件事，做之前先读代码，做完后先编译，修改时手别伸太长。**

这是贯穿整个项目开发过程的基本准则，无论任务大小都应遵守。

### 3.2 一次只做一件事

- 每次只聚焦于一个任务或一个改动点，**不要**在同一个编辑会话中混入多个无关变更
- 完成任务后**先确认**再进入下一项，不要在未验证当前改动的情况下继续叠加
- 多个任务需要并行推进时，应明确拆分后再逐个完成
- **禁止**借"顺手改一下"之名扩大改动范围

### 3.3 做之前先读代码

- 修改任何文件前，必须先 **Read** 该文件以及相关的引用方
- 通过 `Grep` 全局搜索被修改符号的所有引用，避免遗漏
- 理解现有实现的设计意图，**不要**基于猜测做修改
- 修改公共方法、接口、实体类前，必须明确其调用链

### 3.4 做完后先编译

- 每次修改后必须先 **编译**（后端 `mvn compile` / 前端 `pnpm build` 或 `tsc --noEmit`），确认无语法错误
- 编译通过后，针对改动的部分做必要的 **验证**：
  - 后端：通过 Postman / curl 验证接口
  - 前端：浏览器手动验证或运行相关单测
- 编译/验证通过后才算当前任务完成，才可以汇报
- **禁止**只写代码不验证

### 3.5 修改时手别伸太长

- 修改范围应**严格限定**在本次任务目标内，不要"顺手"重构无关代码
- 发现需要重构的旧代码，**单独记录**，不在本次任务中处理
- 不要为了"统一风格"批量重命名无关变量、调整无关格式
- 改动越聚焦，Review 越容易，风险越可控
- **禁止**在 Bug 修复中夹带功能新增；在功能开发中夹带 Bug 修复 —— 应该拆成两次提交

### 3.6 与 Git / Review 的协同

- 一次提交对应一次单一目的的改动（`feat: ...` / `fix: ...` / `refactor: ...`）
- 提交前自查：编译通过、命名规范、异常处理、无调试代码
- 提交信息应清晰描述本次变更的**唯一目标**

---

## 四、命名规范

### 4.1 Java 后端

- **包名**：全小写，使用 `.` 分隔，如 `com.learngen.controller`
- **类名**：UpperCamelCase，如 `ProfileAgent`、`ChatController`
- **接口名**：UpperCamelCase，**不使用** `I` 前缀，如 `ResourceService`
- **方法名**：lowerCamelCase，如 `getStudentProfile()`、`sendMessage()`
- **常量名**：UPPER_SNAKE_CASE，如 `MAX_RETRY_COUNT`
- **变量名**：lowerCamelCase，如 `studentId`、`agentType`
- **数据库字段**：lower_snake_case，如 `student_id`、`created_at`
- **URL 路径**：kebab-case，如 `/api/learning-resources`
- **配置 key**：kebab-case 或 lowerCamelCase，与 Spring Boot 约定一致

### 4.2 Vue 3 前端

- **文件名**：
  - 组件文件：PascalCase，如 `ChatPanel.vue`、`ResourceCard.vue`
  - 页面文件：PascalCase，如 `ChatPage.vue`、`ProfilePage.vue`
  - 组合式函数（Composables）：`useXxx.ts`，如 `useWebSocket.ts`
  - 工具函数：camelCase，如 `formatDate.ts`
  - 类型定义：PascalCase 或 camelCase，如 `student.ts`、`resourceType.ts`
- **组件名**：PascalCase，与文件名一致
- **变量/函数**：camelCase
- **常量**：UPPER_SNAKE_CASE
- **CSS 类名**：kebab-case，使用 BEM 或 scoped style
- **路由路径**：kebab-case，如 `/resource-center`

### 4.3 通用

- **禁止**使用拼音命名
- **禁止**使用无意义的缩写（如 `stu`、`res`），完整单词优先
- 命名字段和变量时语义清晰，见名知义

---

## 五、模块划分

### 5.1 后端包结构

```
com.learngen
├── controller/          # REST API + WebSocket 控制器
│   ├── ChatController.java
│   ├── ProfileController.java
│   ├── ResourceController.java
│   └── PathController.java
├── agent/               # 8 个 Agent + 编排器
│   ├── AgentBase.java           # Agent 抽象基类
│   ├── AgentMessage.java        # 消息体
│   ├── Orchestrator.java        # 编排器
│   ├── ProfileAgent.java        # 画像 Agent
│   ├── DocAgent.java            # 文档 Agent
│   ├── MindMapAgent.java        # 思维导图 Agent
│   ├── QuizAgent.java           # 题库 Agent
│   ├── ReadingAgent.java        # 阅读 Agent
│   ├── CodeCaseAgent.java       # 实操 Agent
│   ├── PathAgent.java           # 路径 Agent
│   └── TutorAgent.java          # 辅导 Agent（加分）
├── service/             # 业务服务
│   ├── ChatService.java
│   ├── ProfileService.java
│   ├── ResourceService.java
│   ├── KnowledgeBaseService.java
│   └── RecommendService.java
├── ai/                  # AI 调用层
│   ├── SparkClient.java         # 讯飞星火 API 封装
│   └── PromptTemplates.java     # Prompt 模板管理
├── model/               # 实体类（MyBatis-Plus）
│   ├── StudentProfile.java
│   ├── LearningResource.java
│   ├── LearningPath.java
│   ├── ChatMessage.java
│   └── KnowledgePoint.java
├── mapper/              # MyBatis-Plus Mapper
│   ├── StudentProfileMapper.java
│   ├── ResourceMapper.java
│   └── PathMapper.java
└── config/              # 配置类
    ├── WebSocketConfig.java
    ├── CorsConfig.java
    └── AIConfig.java
```

### 5.2 前端目录结构

```
src/
├── pages/                   # 页面级组件
│   ├── ChatPage/            # 对话页（核心入口）
│   ├── ProfilePage/         # 画像展示页
│   ├── ResourceCenter/      # 资源中心
│   ├── LearningPath/        # 学习路径页
│   ├── TutorPage/           # 辅导页（加分）
│   └── Dashboard/           # 学习仪表盘（加分）
├── components/              # 可复用组件
│   ├── ChatMessage/         # 聊天消息组件
│   ├── MarkdownRenderer/    # Markdown 渲染
│   ├── MindMapView/         # 思维导图（markmap）
│   ├── QuizCard/            # 题目卡片
│   ├── CodeViewer/          # 代码展示（Monaco Editor）
│   ├── ResourceCard/        # 资源卡片（5 种类型统一）
│   ├── ProfileRadar/        # 6 维雷达图
│   ├── PathTimeline/        # 学习路径时间线
│   ├── ProgressTracker/     # 生成进度追踪
│   └── RecommendPanel/      # 推荐面板
├── composables/             # 组合式函数
│   ├── useWebSocket.ts
│   ├── useChat.ts
│   └── useResource.ts
├── stores/                  # Pinia 状态管理
├── services/                # API 调用封装（Axios）
├── router/                  # Vue Router 路由配置
├── types/                   # TypeScript 类型定义
└── utils/                   # 工具函数
```

### 5.3 8 个 Agent 说明

| Agent | 角色名 | 核心职责 |
|-------|--------|---------|
| ProfileAgent | 学习分析师 | 对话抽取学生 6 维画像 |
| DocAgent | 课程讲师 | 生成课程讲解文档（Markdown） |
| MindMapAgent | 知识架构师 | 生成知识点思维导图（JSON 树） |
| QuizAgent | 出题专家 | 生成练习题库（结构化 JSON） |
| ReadingAgent | 学术推荐官 | 推荐拓展阅读材料 |
| CodeCaseAgent | 实战教练 | 生成代码实操案例 |
| PathAgent | 学习规划师 | 规划个性化学习路径 |
| TutorAgent | 辅导老师 | 即时答疑解惑（加分项） |

---

## 六、数据库规范

### 6.1 通用规范

- 使用 MySQL 8.0，存储引擎统一使用 InnoDB
- 字符集使用 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`
  - **JDBC URL 中 `characterEncoding` 必须写 `UTF-8`**（JDK 标准字符集名），驱动内部映射到 MySQL 的 `utf8mb4`。
    写 `characterEncoding=utf8mb4` 会触发 `UnsupportedEncodingException`（8.3.0+ 严格校验）。
  - 建表 SQL 的 `CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` 是 MySQL 服务端语法，写法不变。
- 所有表必须有 `COMMENT` 注释
- 所有字段必须有 `COMMENT` 注释
- 主键统一使用 `BIGINT PRIMARY KEY AUTO_INCREMENT`
- 时间字段使用 `DATETIME` 类型
- 必须为外键和查询条件字段建立索引
- JSON 类型字段用于存储结构化但 schema 多变的数据（如画像维度、资源内容）
- **禁止**在业务代码中拼接 SQL，使用 MyBatis-Plus 的 LambdaQueryWrapper
- 索引命名：普通索引 `idx_字段名`，唯一索引 `uk_字段名`

### 6.2 数据库表

系统共 6 张表：

| 表名 | 说明 |
|------|------|
| student | 学生信息表 |
| student_profile | 学习画像表（6 维度，JSON 存储） |
| chat_message | 对话记录表 |
| learning_resource | 学习资源表（5 种类型，JSON 存储内容） |
| learning_path | 学习路径表（JSON 存储路径数据） |
| learning_record | 学习记录表（效果评估用） |
| knowledge_point | 知识点元数据表（正文 Markdown 位于 classpath:`resources/knowledge/`） |

### 6.3 实体类规范

- 使用 MyBatis-Plus 的 `@TableName` 注解绑定表名
- 主键使用 `@TableId(type = IdType.AUTO)`
- 使用 `@Data`（Lombok）简化 getter/setter
- JSON 字段在实体类中以 `String` 类型存储，Service 层负责序列化/反序列化

---

## 七、Redis 规范

> 本项目开发阶段以 MySQL 为主，Redis 作为可选缓存层，用于提升性能。

- Key 命名：`项目前缀:模块:业务标识`，如 `learngen:profile:1`、`learngen:session:abc123`
- 必须设置过期时间，禁止使用永久 Key
- 缓存用途：
  - 学生画像缓存（减少画像查询）
  - 对话 Session 上下文（WebSocket 会话关联）
  - 知识库热点数据缓存
  - 流式生成临时状态
- 序列化：统一使用 JSON 格式，配置 Jackson2JsonRedisSerializer
- **禁止**将 Redis 作为主存储，数据必须以 MySQL 为准

---

## 八、异常规范

### 8.1 异常体系

- 项目统一使用 `BusinessException`（自定义运行时异常）作为业务异常
- 通过 `GlobalExceptionHandler`（`@RestControllerAdvice`）全局捕获处理
- 异常分类：
  - 业务异常（`BusinessException`）：可预期的错误，如"画像不存在"
  - 系统异常（`RuntimeException`）：不可预期的运行时错误
  - AI 服务异常（`AIServiceException`）：讯飞 API 调用失败、超时、返回异常

### 8.2 异常处理要求

- Controller 层**禁止** try-catch，由全局异常处理器统一处理
- Service 层在需要抛业务异常时，抛出 `BusinessException`
- 调用讯飞 API 时必须捕获网络异常并包装为 `AIServiceException`
- 所有异常必须有明确的错误消息，**禁止**返回空消息
- 日志级别：业务异常 `warn`，系统异常 `error`

---

## 九、返回结果规范

### 9.1 统一响应体

所有 REST API 使用统一响应结构：

```java
public class Result<T> {
    private int code;        // 状态码
    private String message;  // 提示信息
    private T data;          // 数据体
}
```

### 9.2 状态码约定

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 9.3 分页响应

分页接口统一返回：

```java
public class PageResult<T> {
    private List<T> records;     // 数据列表
    private long total;          // 总记录数
    private int page;            // 当前页
    private int pageSize;        // 每页大小
}
```

### 9.4 要求

- Controller 方法返回值统一包裹为 `Result<T>`
- **禁止**直接返回实体对象或 Map
- 创建/更新操作返回操作后的完整数据，而非仅返回 id
- WebSocket 消息也遵循类似结构：`{ type, payload, timestamp }`

---

## 十、Controller 规范

### 10.1 注解使用

- 类上使用 `@RestController` + `@RequestMapping("/api/xxx")`
- 使用 `@Slf4j` 记录日志
- 方法上使用对应的 HTTP 方法注解：`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`
- 参数校验使用 `@Valid` 或 `@Validated`
- 路径参数使用 `@PathVariable`，查询参数使用 `@RequestParam`，请求体使用 `@RequestBody`

### 10.2 URL 设计

- RESTful 风格：资源名用复数名词
- 示例：
  - `GET /api/profiles/{studentId}` —— 查询画像
  - `PUT /api/profiles/{studentId}` —— 更新画像
  - `GET /api/resources?type=doc` —— 查询资源列表
  - `POST /api/resources/generate` —— 触发生成资源

### 10.3 控制器职责

- Controller 层**只做**：参数校验、调用 Service、返回结果
- **禁止**在 Controller 中写业务逻辑
- **禁止**在 Controller 中直接调用 Mapper
- **禁止**在 Controller 中直接调用 Agent（应通过 Orchestrator 或 Service）
- WebSocket Controller（ChatController）负责消息收发，具体逻辑委托给 ChatService

---

## 十一、Service 规范

### 11.1 接口与实现

- Service 层使用接口 + 实现类模式（`XxxService` 接口 + `XxxServiceImpl` 实现）
- 接口中写 Javadoc，实现类上加 `@Service` 注解
- 使用 `@Slf4j` 记录关键日志

### 11.2 事务

- 需要事务的方法上使用 `@Transactional(rollbackFor = Exception.class)`
- 读操作**不需要**事务注解
- **禁止**在事务中调用外部 API（如讯飞 API），避免长事务

### 11.3 职责

- Service 层负责：业务逻辑编排、调用 Mapper、调用 Agent
- **禁止**在 Service 中直接操作 HttpServletRequest/HttpServletResponse
- 画像更新、资源生成等核心逻辑放在 Service 层

---

## 十二、Mapper 规范

### 12.1 基础要求

- 使用 MyBatis-Plus 的 `BaseMapper<T>` 作为父接口
- 简单 CRUD 使用 MyBatis-Plus 内置方法
- 复杂查询使用 `LambdaQueryWrapper` 构建条件
- **禁止**在业务代码中拼接 SQL 字符串
- 如需自定义 SQL，写在对应的 XML 文件中（`resources/mapper/`）
- 自定义 SQL 的 XML 文件名与 Mapper 接口名一致

### 12.2 示例

```java
// ✅ 正确
LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(LearningResource::getStudentId, studentId)
       .eq(LearningResource::getType, type)
       .orderByDesc(LearningResource::getCreatedAt);

// ❌ 禁止
String sql = "SELECT * FROM learning_resource WHERE student_id = " + studentId;
```

---

## 十三、Docker 规范

> 用于开发环境快速搭建和最终部署。

- MySQL 使用 Docker 容器运行，数据挂载到本地目录
- 后端使用 `Dockerfile` 构建镜像（多阶段构建，基于 `eclipse-temurin:17-jre`）
- 前端构建产物使用 Nginx 镜像部署
- 使用 `docker-compose.yml` 编排整体服务
- `.dockerignore` 排除 `target/`、`node_modules/`、`.git/` 等
- 容器内 JVM 参数：`-Xms256m -Xmx512m`（开发）/ `-Xms512m -Xmx1024m`（演示）
- **禁止**将数据库凭据、API Key 硬编码在 Dockerfile 中，使用环境变量

---

## 十四、Maven 规范

- Group ID：`com.learngen`
- Artifact ID：`learngen-backend`
- 版本号：`1.0.0-SNAPSHOT`（开发）/ `1.0.0`（发布）
- 统一管理依赖版本：在 `<properties>` 中声明版本号
- 依���顺序：按 scope 分组（compile → runtime → test → provided）
- **禁止**引入未使用的依赖
- 在 `pom.xml` 中配置 `maven-compiler-plugin`，指定 Java 17

---

## 十五、禁止事项

### 绝对禁止

1. **禁止**将 API Key、数据库密码等敏感信息提交到代码仓库（使用 `application.yml` 外置配置 + 环境变量）
2. **禁止**在生产配置中开启 `spring.jpa.show-sql` 或打印 SQL 的调试日志
3. **禁止**使用 `System.out.println()` 打印日志（统一使用 `@Slf4j` + `log.info/warn/error`）
4. **禁止**捕获异常后不做任何处理（空 catch 块）
5. **禁止**在循环中执行 SQL 查询（使用批量查询或 IN 条件）
6. **禁止**使用 `Thread.sleep()` 进行延时控制
7. **禁止**在 Controller 中直接返回 `null`
8. **禁止**使用 `@Autowired` 字段注入（使用构造器注入或 Lombok `@RequiredArgsConstructor`）
9. **禁止**在 Vue 组件中直接操作 DOM（使用 Vue 的响应式绑定）
10. **禁止**提交 `console.log()` 调试代码到前端代码仓库

### 应当避免

- 避免在前端存储大段 AI 生成的原始 JSON（后端做精简后再返回）
- 避免在 Agent 的 System Prompt 中硬编码临时性内容（应使用 `PromptTemplates` 统一管理）
- 避免超过 200 行的 Service 方法（应拆分）
- 避免超过 500 行的 Vue 组件（应拆分为子组件或 Composables）

---

## 十六、代码 Review 规范

### 16.1 Review 流程

1. 代码提交前自检：确认无编译错误、无 lint 报错
2. 至少一人 Review 后方可合并
3. Review 重点检查：
   - 功能逻辑是否正确
   - 异常处理是否完善
   - 数据库操作是否符合规范
   - 命名是否符合规范
   - 是否有潜在性能问题（N+1 查询等）

### 16.2 Review 清单

- [ ] 代码符合命名规范
- [ ] 异常处理完善（调讯飞 API 有超时处理）
- [ ] 无硬编码敏感信息
- [ ] Controller 职责清晰（无业务逻辑）
- [ ] 返回结果统一使用 `Result<T>`
- [ ] 数据库操作使用 LambdaQueryWrapper，无 SQL 拼接
- [ ] 无 debug 代码（console.log / System.out.println）
- [ ] 新增文件符合目录结构规范
- [ ] Prompt 模板变更需同步更新 `PromptTemplates.java`

---

## 十七、新增功能流程

1. **需求确认**：在相应模块文档中明确功能目标和验收标准
2. **设计先行**：
   - 后端：确定 API 路径、请求/响应结构、涉及的 Agent/Service/Mapper
   - 前端：确定页面路径、组件拆分、状态管理方案
3. **后端开发**：
   - 实体类 / 建表 SQL → Mapper → Service → Controller → 联调
   - Agent 开发：实现 `AgentBase` → 编写 System Prompt → 注入 Orchestrator
4. **前端开发**：
   - 类型定义 → API Service → Pinia Store → 页面组件 → 路由注册
5. **自测**：使用 Postman（后端）/ 浏览器 DevTools（前端）验证
6. **联调**：前后端对接，确认数据格式一致
7. **Review**：提交代码 Review（参考第十五条）
8. **更新文档**：如有架构变更，同步更新项目文档

---

## 十八、修改已有代码流程

1. **理解现状**：阅读要修改的代码及相关联的代码，确认影响范围
2. **确认修改方案**：明确改动点和改动方式
3. **局部验证**：修改后先确认修改部分逻辑正确
4. **回归确认**：检查是否有其他代码依赖被修改的部分（Ctrl+Shift+F 全局搜索引用）
5. **Review**：提交 Review
6. **更新文档**：如有接口变更，同步更新文档

### 修改 Agent 时的额外要求

- 修改 Agent System Prompt 时，需在 `PromptTemplates.java` 中统一管理
- 修改 Orchestrator 意图路由规则时，需确保覆盖所有 Agent 的触发场景
- 修改 Agent 输出格式时，需同步通知前端对应的解析逻辑

### 修改数据库时的额外要求

- 修改表结构需提供 DDL 变更脚本（增量 ALTER 语句）
- 修改 JSON 字段内部结构时，需前后端同步对齐解析逻辑

---

## 十九、前端 Vue 3 特有规范

### 19.1 组件编写

- 使用 `<script setup lang="ts">` 语法
- 使用 Composition API（`ref`、`computed`、`watch`、`onMounted` 等）
- 组件 props 使用 `defineProps<T>()` 配合 TypeScript 类型
- 组件 emits 使用 `defineEmits<T>()`
- 样式使用 `<style scoped lang="scss">`

### 19.2 状态管理

- 全局状态使用 Pinia
- 组件内部状态使用 `ref` / `reactive`
- API 请求状态统一管理：`loading`、`error`、`data`

### 19.3 路由

- 使用 Vue Router 4，路由配置集中管理
- 路由懒加载：`() => import('@/pages/XxxPage/XxxPage.vue')`

### 19.4 API 调用

- 统一封装 Axios 实例，配置 baseURL、拦截器
- 请求拦截器：添加通用 Header
- 响应拦截器：统一错误提示（ElMessage）

---

## 二十、讯飞 API 调用规范

- 所有讯飞 API 调用必须通过 `SparkClient` 统一入口
- **主备切换（§20.1）**：`SparkRouter`（`@Primary` Bean）管理 primary/secondary 两个 `SparkClientImpl`；
  默认走 primary；遭遇限流 (`10110/11201-11203`)、鉴权 (`10015/10016/11200`)、网络 IO 异常时自动切到 secondary，
  每请求最多切一次。secondary 未配置时降级为单 key 行为。
- 调用前检查 API Key 是否配置
- 必须设置超时时间（连接超时 10s，读取超时 60s）
- 必须处理以下异常场景：网络超时、API Key 无效、模型繁忙/限流、返回空内容
- 流式输出使用 SSE 方式，边接收边通过 WebSocket 推送给前端
- 在 `PromptTemplates.java` 中统一管理所有 Prompt 模板
- 防幻觉机制：对生成内容做关键词校验 + 知识库事实核对

---

## 二十一、5 种资源类型数据结构

| 类型 | type 值 | 内容结构 |
|------|---------|---------|
| 课程讲解文档 | `doc` | `{ markdown, summary }` |
| 知识点思维导图 | `mindmap` | `{ tree: { name, children[] } }` |
| 练习题库 | `quiz` | `{ questions: [{ type, question, options[], answer, explanation }] }` |
| 拓展阅读材料 | `reading` | `{ items: [{ title, url, type, difficulty, reason }] }` |
| 代码实操案例 | `code` | `{ description, dataset, code, expected_output, explanation }` |

---

## 二十二、WebSocket 通信规范

### 22.1 消息结构

```json
{
  "type": "message|progress|resource|error|done",
  "payload": {},
  "timestamp": "ISO 字符串"
}
```

### 22.2 消息类型

| type | 说明 |
|------|------|
| `message` | 普通对话消息（流式片段） |
| `progress` | 资源生成进度更新 |
| `resource` | 资源生成完成，推送结果 |
| `error` | 错误消息 |
| `done` | 流式输出结束信号 |

### 22.3 要求

- 前端使用原生 WebSocket 或封装 composable
- 必须处理断线重连（指数退避策略）
- 后端通过 STOMP 代理或自定义 WebSocket Handler

---
