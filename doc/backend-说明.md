# 后端说明（Spring Boot）

> 本文档说明本项目后端部分的搭建、开发、模块划分和约束。后端位于仓库 `backend/` 目录下，所有代码遵循 [CLAUDE.md](./CLAUDE.md) 的开发规范。

---

## 一、技术栈

| 技术                     | 版本  | 用途                             |
| ------------------------ | ----- | -------------------------------- |
| Spring Boot              | 3.2.x | 主框架                           |
| JDK                      | 17    | Java 版本                        |
| MyBatis-Plus             | 3.5.x | ORM                              |
| MySQL                    | 8.0   | 主数据库                         |
| Spring WebSocket + STOMP | —    | 实时通信（对话流式）             |
| OkHttp                   | 4.x   | HTTP 客户端（讯飞 API SSE 流式） |
| Maven                    | 3.9+  | 构建工具                         |
| Lombok                   | —    | 简化 POJO                        |

---

## 二、环境搭建

> 完整的开发环境配置（数据库、Redis、后端、前端）见 [dev-env.md](./dev-env.md)。本节仅列出后端特有的搭建步骤。

### 2.1 初始化

```bash
cd backend
mvn -v   # 确认 JDK 17 + Maven 3.9+
mvn spring-boot:run
```

### 2.2 Maven 项目结构

```
backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/learngen/
    │   │   ├── LearngenApplication.java
    │   │   ├── controller/
    │   │   ├── agent/
    │   │   ├── service/
    │   │   ├── ai/
    │   │   ├── model/
    │   │   ├── mapper/
    │   │   ├── config/
    │   │   └── exception/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── knowledge/        # ML 知识库 Markdown
    │       ├── prompts/          # Prompt 模板
    │       └── mapper/           # 自定义 SQL XML
    └── test/
        └── java/com/learngen/
```

### 2.3 pom.xml 关键配置

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.5</spring-boot.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <okhttp.version>4.12.0</okhttp.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>${okhttp.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

> 修改完代码后必须先 `mvn compile` 确认编译通过，符合 CLAUDE.md §3.4。

---

## 三、模块划分（包结构）

```
com.learngen
├── LearngenApplication.java      # 启动类
├── controller/                   # REST API + WebSocket
│   ├── ChatController.java       # 对话 WebSocket
│   ├── ProfileController.java    # 画像 CRUD
│   ├── ResourceController.java   # 资源生成/查询
│   └── PathController.java       # 学习路径
├── agent/                        # 8 个 Agent + 编排器
│   ├── AgentBase.java
│   ├── AgentMessage.java
│   ├── Orchestrator.java
│   ├── ProfileAgent.java
│   ├── DocAgent.java
│   ├── MindMapAgent.java
│   ├── QuizAgent.java
│   ├── ReadingAgent.java
│   ├── CodeCaseAgent.java
│   ├── PathAgent.java
│   └── TutorAgent.java
├── service/                      # 业务层
│   ├── ChatService.java
│   ├── ProfileService.java
│   ├── ResourceService.java
│   ├── KnowledgeBaseService.java
│   └── RecommendService.java
├── ai/                           # AI 调用层
│   ├── SparkClient.java          # 讯飞星火 SSE 流式
│   └── PromptTemplates.java      # Prompt 集中管理
├── model/                        # 实体类
│   ├── StudentProfile.java
│   ├── LearningResource.java
│   ├── LearningPath.java
│   ├── ChatMessage.java
│   └── KnowledgePoint.java
├── mapper/                       # MyBatis-Plus Mapper
│   ├── StudentProfileMapper.java
│   ├── ResourceMapper.java
│   ├── PathMapper.java
│   └── ChatMessageMapper.java
├── config/                       # 配置类
│   ├── WebSocketConfig.java
│   ├── CorsConfig.java
│   ├── AIConfig.java
│   └── MybatisPlusConfig.java
└── exception/                    # 异常体系
    ├── BusinessException.java
    ├── AIServiceException.java
    └── GlobalExceptionHandler.java
```

---

## 四、命名规范

| 类型       | 命名                                    | 示例                           |
| ---------- | --------------------------------------- | ------------------------------ |
| 包名       | 全小写，`.` 分隔                      | `com.learngen.controller`    |
| 类名       | UpperCamelCase                          | `ProfileAgent`               |
| 接口名     | UpperCamelCase，**无** `I` 前缀 | `ResourceService`            |
| 方法名     | lowerCamelCase                          | `getStudentProfile()`        |
| 常量       | UPPER_SNAKE_CASE                        | `MAX_RETRY_COUNT`            |
| 变量       | lowerCamelCase                          | `studentId`、`agentType`   |
| 数据库字段 | lower_snake_case                        | `student_id`、`created_at` |
| URL 路径   | kebab-case                              | `/api/learniexung-resources` |
| 配置 key   | kebab-case / lowerCamelCase             | `spring.datasource.url`      |

**禁止**：

- 拼音命名
- 无意义缩写（`stu`、`res` 等）
- 接口加 `I` 前缀
- 字段使用 `UserName` 这种混乱大小写

---

## 五、数据库设计

### 5.1 数据库与表

MySQL 8.0，存储引擎 InnoDB，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`。

系统共 6 张表：

| 表名                  | 说明                                |
| --------------------- | ----------------------------------- |
| `student`           | 学生信息                            |
| `student_profile`   | 学习画像（6 维，JSON 存储）         |
| `chat_message`      | 对话记录                            |
| `learning_resource` | 学习资源（5 种类型，JSON 存储内容） |
| `learning_path`     | 学习路径（JSON 存储路径数据）       |
| `learning_record`   | 学习记录（效果评估）                |

完整建表 SQL 见 [04-数据库设计.md](./04-数据库设计.md)。

### 5.2 表设计约束

- 所有表必须有 `COMMENT`
- 所有字段必须有 `COMMENT`
- 主键统一 `BIGINT PRIMARY KEY AUTO_INCREMENT`
- 时间字段统一 `DATETIME`
- 外键 / 查询条件字段必建索引
- 索引命名：普通索引 `idx_字段名`，唯一索引 `uk_字段名`
- JSON 字段：内部结构 schema 多变的数据（如画像维度、资源内容）使用 JSON 类型

### 5.3 实体类

```java
@Data
@TableName("student_profile")
public class StudentProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    // JSON 字段以 String 存储，Service 层负责序列化
    private String knowledgeBase;
    private String cognitiveStyle;
    private String learningGoal;
    private String weakPoints;
    private String learningPace;
    private String interestArea;
    private LocalDateTime updatedAt;
}
```

---

## 六、Mapper 规范

### 6.1 基础

- 继承 `BaseMapper<T>`，使用 MyBatis-Plus 内置 CRUD
- 复杂查询使用 `LambdaQueryWrapper`
- **禁止**拼接 SQL 字符串
- 自定义 SQL 写在 `resources/mapper/` 下，XML 文件名与 Mapper 接口名一致

### 6.2 示例

```java
// ✅ 推荐
LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(LearningResource::getStudentId, studentId)
       .eq(LearningResource::getType, type)
       .orderByDesc(LearningResource::getCreatedAt);
List<LearningResource> list = resourceMapper.selectList(wrapper);

// ❌ 禁止
String sql = "SELECT * FROM learning_resource WHERE student_id = " + studentId;
```

### 6.3 性能约束

- **禁止**在循环中执行 SQL 查询（应批量查询或 IN 条件）
- 避免 N+1 查询：连表 / 批量取数据后在内存组装
- JSON 字段查询必要时建函数索引（MySQL 8.0 支持）

---

## 七、Service 规范

### 7.1 接口 + 实现

```java
// ResourceService.java
public interface ResourceService {
    Result<LearningResource> generate(Long studentId, String type, String knowledgePoint);
    Result<List<LearningResource>> list(Long studentId, String type);
    Result<LearningResource> getById(Long id);
}

// ResourceServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final Orchestrator orchestrator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<LearningResource> generate(Long studentId, String type, String knowledgePoint) {
        // 1. 调 Agent 生成
        AgentMessage result = orchestrator.dispatch(type, AgentMessage.of(...));
        // 2. 持久化
        LearningResource resource = new LearningResource();
        // ... 填充字段
        resourceMapper.insert(resource);
        return Result.success(resource);
    }
}
```

### 7.2 约束

- 必须使用接口 + 实现类
- 使用 `@RequiredArgsConstructor`（Lombok）做构造器注入，**禁止** `@Autowired` 字段注入
- 需要事务的方法加 `@Transactional(rollbackFor = Exception.class)`
- 读操作不加事务注解
- **禁止**在事务中调用外部 API（讯飞 API）
- 单一 Service 方法不超过 200 行（超过则拆分）
- 不在 Service 中直接操作 `HttpServletRequest/Response`

---

## 八、Controller 规范

### 8.1 REST 控制器

```java
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{studentId}")
    public Result<StudentProfile> getProfile(@PathVariable Long studentId) {
        return profileService.getByStudentId(studentId);
    }

    @PutMapping("/{studentId}")
    public Result<StudentProfile> updateProfile(
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(studentId, request);
    }
}
```

### 8.2 URL 设计

- RESTful：资源名用复数名词
- 示例：
  - `GET /api/profiles/{studentId}`
  - `PUT /api/profiles/{studentId}`
  - `GET /api/resources?type=doc`
  - `POST /api/resources/generate`

### 8.3 职责边界

- Controller **只做**：参数校验、调用 Service、返回结果
- **禁止**：业务逻辑、直接调用 Mapper、直接调用 Agent
- 返回值统一包裹为 `Result<T>`，**禁止**直接返回实体
- 创建/更新操作返回操作后的完整数据

### 8.4 WebSocket 控制器

```java
@Controller
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatRequest request, SimpMessagingHeaderAccessor header) {
        chatService.handleStream(request, header);
    }
}
```

---

## 九、统一响应体

```java
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

### 状态码

| code | 含义           |
| ---- | -------------- |
| 200  | 成功           |
| 400  | 请求参数错误   |
| 401  | 未认证         |
| 404  | 资源不存在     |
| 500  | 服务器内部错误 |

---

## 十、异常规范

### 10.1 异常体系

```
RuntimeException
├── BusinessException           # 业务异常（可预期）
├── AIServiceException          # 讯飞 API 异常
└── OtherRuntimeException       # 系统异常（不可预期）
```

### 10.2 全局处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(AIServiceException.class)
    public Result<Void> handleAI(AIServiceException e) {
        log.error("AI 服务异常：{}", e.getMessage(), e);
        return Result.error(500, "AI 服务暂时不可用，请稍后再试");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器内部错误");
    }
}
```

### 10.3 约束

- Controller 层**禁止** try-catch
- Service 层抛 `BusinessException` 时必须有明确错误消息
- 调用讯飞 API 必须捕获网络异常并包装为 `AIServiceException`
- 日志级别：业务异常 `warn`，系统异常 `error`
- **禁止**捕获异常后不做任何处理

---

## 十一、Agent 体系

### 11.1 8 个 Agent

| Agent         | 角色       | 输入            | 输出           |
| ------------- | ---------- | --------------- | -------------- |
| ProfileAgent  | 学习分析师 | 用户聊天文本    | 6 维画像 JSON  |
| DocAgent      | 课程讲师   | 知识点 + 画像   | Markdown 文档  |
| MindMapAgent  | 知识架构师 | 课程大纲        | JSON 树        |
| QuizAgent     | 出题专家   | 知识点 + 难度   | 题目 JSON      |
| ReadingAgent  | 学术推荐官 | 知识点 + 画像   | 阅读推荐列表   |
| CodeCaseAgent | 实战教练   | 知识点 + 画像   | 代码教程       |
| PathAgent     | 学习规划师 | 画像 + 学习记录 | 路径 JSON      |
| TutorAgent    | 辅导老师   | 问题 + 上下文   | 解答（含图解） |

### 11.2 Agent 基类

```java
public abstract class AgentBase {
    protected String name;
    protected String role;
    protected String systemPrompt;

    public abstract AgentMessage process(AgentMessage input);
}
```

### 11.3 编排器（Orchestrator）

```java
@Component
public class Orchestrator {
    private final Map<String, AgentBase> agentMap;

    public Orchestrator(List<AgentBase> agents) {
        // Spring 自动注入所有 AgentBase Bean
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(AgentBase::getName, a -> a));
    }

    public AgentMessage dispatch(String intent, AgentMessage input) {
        AgentBase agent = routeByIntent(intent);
        return agent.process(input);
    }
}
```

**设计亮点**：Spring 自动注入所有 `AgentBase` 实现类，新增 Agent 只需加 `@Component` 即可注册。

### 11.4 Prompt 管理

- 所有 Prompt 集中在 `ai/PromptTemplates.java` 中管理
- **禁止**在 Agent 中硬编码临时性 Prompt
- 修改 Prompt 需同步通知前端对应的解析逻辑

---

## 十二、讯飞 API 调用规范

### 12.1 SparkClient 统一入口

所有讯飞 API 调用必须通过 `ai/SparkClient.java`：

```java
@Component
@Slf4j
public class SparkClient {

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String apiSecret;

    public SparkClient(@Value("${spark.api-key}") String apiKey,
                       @Value("${spark.api-secret}") String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /** 流式调用，通过回调逐片返回 */
    public void streamChat(String systemPrompt, String userInput,
                           Consumer<String> onChunk,
                           Runnable onDone,
                           Consumer<Throwable> onError) {
        // 1. 鉴权（HMAC-SHA256 签名）
        // 2. 发起 SSE 请求
        // 3. 解析 SSE 流，回调 onChunk
    }
}
```

### 12.2 约束

- 调用前检查 API Key 是否配置
- 连接超时 10s、读取超时 60s
- 必须处理：网络超时、API Key 无效、模型繁忙/限流、返回空内容
- 流式输出：边接收边通过 WebSocket 推送给前端
- 防幻觉：生成内容做关键词校验 + 知识库事实核对
- 敏感信息（API Key）使用 `application.yml` 外置配置 + 环境变量，**禁止**提交到仓库

---

## 十三、WebSocket 通信

### 13.1 消息结构

```json
{
  "type": "message|progress|resource|error|done",
  "payload": {},
  "timestamp": "ISO 字符串"
}
```

### 13.2 消息类型

| type         | 说明         |
| ------------ | ------------ |
| `message`  | 对话流式片段 |
| `progress` | 资源生成进度 |
| `resource` | 资源生成完成 |
| `error`    | 错误消息     |
| `done`     | 流式输出结束 |

### 13.3 配置

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
```

---

## 十四、Redis 规范（可选）

- Key 命名：`learngen:模块:业务标识`，如 `learngen:profile:1`
- 必须设置过期时间，禁止永久 Key
- 用途：画像缓存、Session 上下文、知识库热点、流式生成临时状态
- 序列化：JSON 格式，Jackson2JsonRedisSerializer
- **禁止**将 Redis 作为主存储，MySQL 为准

---

## 十五、配置管理

### 15.1 application.yml

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/learngen?useUnicode=true&characterEncoding=UTF-8
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

spark:
  api-key: ${SPARK_API_KEY}
  api-secret: ${SPARK_API_SECRET}
  host: spark-api.xf-yun.com
  domain: general

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: AUTO

logging:
  level:
    com.learngen: debug
```

### 15.2 环境隔离

- `application-dev.yml`：开发环境
- `application-prod.yml`：生产环境
- 敏感信息通过环境变量注入，**禁止**硬编码

---

## 十六、Docker 部署

### 16.1 Dockerfile（多阶段构建）

```dockerfile
# 构建阶段
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 16.2 docker-compose.yml

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: learngen
    ports:
      - "3306:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql

  backend:
    build: ./backend
    depends_on:
      - mysql
    environment:
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD}
      SPARK_API_KEY: ${SPARK_API_KEY}
      SPARK_API_SECRET: ${SPARK_API_SECRET}
    ports:
      - "8080:8080"
```

### 16.3 约束

- **禁止**将数据库凭据、API Key 硬编码在 Dockerfile 中
- `.dockerignore` 排除 `target/`、`.git/`、`node_modules/`
- JVM 参数：开发 `-Xms256m -Xmx512m`，演示 `-Xms512m -Xmx1024m`

---

## 十七、禁止事项

| #  | 规则                                              |
| -- | ------------------------------------------------- |
| 1  | 禁止将 API Key、数据库密码硬编码到代码仓库        |
| 2  | 禁止生产配置中开启 SQL 调试日志                   |
| 3  | 禁止使用`System.out.println()`，统一 `@Slf4j` |
| 4  | 禁止空 catch 块                                   |
| 5  | 禁止在循环中执行 SQL 查询                         |
| 6  | 禁止使用`Thread.sleep()` 延时                   |
| 7  | 禁止 Controller 返回`null`                      |
| 8  | 禁止`@Autowired` 字段注入                       |
| 9  | 禁止 SQL 字符串拼接                               |
| 10 | 禁止在事务中调用外部 API                          |

---

## 十八、提交前自检清单

- [ ] `mvn compile` 无报错
- [ ] `mvn test` 通过
- [ ] 无 `System.out.println` 残留
- [ ] 无硬编码的 API Key / 数据库密码
- [ ] 所有 Controller 方法返回 `Result<T>`
- [ ] 所有数据库操作使用 LambdaQueryWrapper
- [ ] 所有异常均有明确消息
- [ ] `application.yml` 通过环境变量注入敏感配置
- [ ] Dockerfile 不含凭据
- [ ] 8 个 Agent 全部注册到 Orchestrator
- [ ] Prompt 模板集中在 `PromptTemplates.java`
- [ ] WebSocket 流式输出可正确推到前端
- [ ] 提交材料符合 [06-提交材料清单.md](./06-提交材料清单.md)
