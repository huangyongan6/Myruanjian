package com.learngen.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 讯飞星火 Spark 客户端 Bean 注册（CLAUDE.md §20.1：主备切换）。
 *
 * <p>注册三个 Bean：
 * <ul>
 *   <li>{@code primarySparkClient} — 主账号（读 {@code spark.primary.*}）</li>
 *   <li>{@code secondarySparkClient} — 备用账号（读 {@code spark.secondary.*}，password 为空时不注入 Router）</li>
 *   <li>{@code sparkRouter}（{@code @Primary}）— 门面，8 个 Agent 的注入入口</li>
 * </ul>
 *
 * <p>模型字段在两个 slot 上独立覆盖（{@code spark.primary.model} / {@code spark.secondary.model}），
 * 未设置时回退到顶层 {@code spark.model}。回退在 Bean 工厂内手动处理，避开 Spring 占位符嵌套解析的坑。
 */
@Configuration
public class SparkClientConfig {

    @Bean(name = "primarySparkClient")
    public SparkClient primarySparkClient(
            ObjectMapper objectMapper,
            @Value("${spark.host}") String host,
            @Value("${spark.api-path}") String apiPath,
            @Value("${spark.primary.api-password:please-set-via-env}") String apiPassword,
            @Value("${spark.primary.model:#{'${spark.model:generalv3.5}'}}") String modelRaw,
            @Value("${spark.model:generalv3.5}") String modelFallback,
            @Value("${spark.temperature:0.5}") double temperature,
            @Value("${spark.max-tokens:2048}") int maxTokens,
            @Value("${spark.connect-timeout-seconds:10}") int connectTimeout,
            @Value("${spark.read-timeout-seconds:60}") int readTimeout,
            @Value("${spark.total-timeout-seconds:120}") int totalTimeoutSeconds) {
        String model = isBlankOrUnresolved(modelRaw) ? modelFallback : modelRaw;
        return new SparkClientImpl(objectMapper, host, apiPath, apiPassword, model,
                temperature, maxTokens, connectTimeout, readTimeout, totalTimeoutSeconds);
    }

    @Bean(name = "secondarySparkClient")
    public SparkClient secondarySparkClient(
            ObjectMapper objectMapper,
            @Value("${spark.host}") String host,
            @Value("${spark.api-path}") String apiPath,
            @Value("${spark.secondary.api-password:}") String apiPassword,
            @Value("${spark.secondary.model:#{'${spark.model:generalv3.5}'}}") String modelRaw,
            @Value("${spark.model:generalv3.5}") String modelFallback,
            @Value("${spark.temperature:0.5}") double temperature,
            @Value("${spark.max-tokens:2048}") int maxTokens,
            @Value("${spark.connect-timeout-seconds:10}") int connectTimeout,
            @Value("${spark.read-timeout-seconds:60}") int readTimeout,
            @Value("${spark.total-timeout-seconds:120}") int totalTimeoutSeconds) {
        String model = isBlankOrUnresolved(modelRaw) ? modelFallback : modelRaw;
        return new SparkClientImpl(objectMapper, host, apiPath, apiPassword, model,
                temperature, maxTokens, connectTimeout, readTimeout, totalTimeoutSeconds);
    }

    @Bean
    @Primary
    public SparkClient sparkRouter(
            @Qualifier("primarySparkClient") SparkClient primary,
            @Qualifier("secondarySparkClient") SparkClient secondary,
            @Value("${spark.secondary.api-password:}") String secondaryPassword,
            @Value("${spark.failover.secondary-failure-threshold:3}") int failureThreshold,
            @Value("${spark.failover.secondary-cooldown-seconds:60}") long cooldownSeconds) {
        // secondary password 为空/占位 → 视为未配置，Router 降级为单 key 行为
        SparkClient secondaryOrNull =
                (secondaryPassword != null && !secondaryPassword.isBlank()
                        && !"please-set-via-env".equals(secondaryPassword))
                ? secondary : null;
        return new SparkRouter(primary, secondaryOrNull, failureThreshold, cooldownSeconds);
    }

    private static boolean isBlankOrUnresolved(String s) {
        return s == null || s.isBlank() || s.startsWith("${");
    }
}
