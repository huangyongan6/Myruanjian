# =====================================================================
# 学习多智能体系统 - 后端 Dockerfile（多阶段构建）
# 对应 CLAUDE.md §16 Docker 规范
# 基础镜像：eclipse-temurin:17
# =====================================================================

# --------- 阶段 1：构建 ---------
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# 先复制 pom.xml 以充分利用 Docker 缓存
COPY pom.xml ./
COPY src ./src

# 打包（跳过测试）
RUN ./mvnw -q clean package -DskipTests 2>/dev/null || \
    (echo "未发现 mvnw，使用系统 mvn" && mvn -q clean package -DskipTests)

# --------- 阶段 2：运行 ---------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/learngen-backend-1.0.0-SNAPSHOT.jar app.jar

# 时区与编码
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

# 健康检查：依赖 Spring Boot Actuator（CLAUDE.md §16）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]