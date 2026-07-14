package com.learngen.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SparkClient CLI 验证工具。
 *
 * <p>不依赖 Spring，直接读环境变量构造客户端，发送一条 prompt 验证 APIPassword 是否可用。
 *
 * <p>用法：
 * <pre>
 *   # 编译
 *   mvn -q compile
 *
 *   # 运行（需先设置 SPARK_API_PASSWORD_PRIMARY 或 SPARK_API_PASSWORD）
 *   SPARK_API_PASSWORD_PRIMARY=xxx mvn -q exec:java \
 *     -Dexec.mainClass=com.learngen.ai.SparkClientCli \
 *     -Dexec.classpathScope=compile
 *
 *   # 指定源（primary / secondary）
 *   SPARK_API_PASSWORD_PRIMARY=xxx SPARK_API_PASSWORD_SECONDARY=yyy \
 *     mvn -q exec:java -Dexec.mainClass=com.learngen.ai.SparkClientCli \
 *     -Dexec.classpathScope=compile -Dexec.args="--source=secondary"
 * </pre>
 *
 * <p>对应 CLAUDE.md §19：验证讯飞 API Key / Secret 是否正确。
 */
@Slf4j
public class SparkClientCli {

    public static void main(String[] args) {
        // --source=primary|secondary（默认 primary）
        String source = "primary";
        String prompt = null;
        for (String arg : args) {
            if (arg.startsWith("--source=")) {
                source = arg.substring("--source=".length());
            } else {
                prompt = arg;
            }
        }
        String envVar = "secondary".equals(source)
                ? "SPARK_API_PASSWORD_SECONDARY"
                : "SPARK_API_PASSWORD_PRIMARY";
        String apiPassword = System.getenv(envVar);
        // 兼容旧名 SPARK_API_PASSWORD
        if (apiPassword == null || apiPassword.isBlank()) {
            apiPassword = System.getenv("SPARK_API_PASSWORD");
        }
        if (apiPassword == null || apiPassword.isBlank() || "please-set-via-env".equals(apiPassword)) {
            System.err.println("[ERROR] " + envVar + " 环境变量未设置"
                    + (apiPassword != null ? "" : "（也试过 SPARK_API_PASSWORD，亦未设置）"));
            System.exit(1);
        }

        String host = System.getenv().getOrDefault("SPARK_HOST", "spark-api-open.xf-yun.com");
        String model = System.getenv().getOrDefault("SPARK_MODEL", "generalv3.5");

        SparkClient client = new SparkClientImpl(
                new ObjectMapper(),
                host,
                "/v1/chat/completions",
                apiPassword,
                model,
                0.5,
                1024,
                10, 60, 120
        );

        String finalPrompt = prompt != null
                ? prompt
                : readPromptFromStdin();

        System.out.println("========================================");
        System.out.println(" Source:  " + source + " (" + (source.equals("primary") ? "SPARK_API_PASSWORD_PRIMARY" : "SPARK_API_PASSWORD_SECONDARY") + ")");
        System.out.println(" Prompt: " + finalPrompt);
        System.out.println(" Model:  " + model);
        System.out.println(" Host:   " + host);
        System.out.println("========================================");
        System.out.println(" Response:");

        AtomicReference<String> accumulated = new AtomicReference<>("");
        AtomicReference<Throwable> error = new AtomicReference<>();
        long start = System.currentTimeMillis();

        client.streamChat(
                "你是一个测试助手。请用一句话回答用户的问题，不要使用 Markdown。",
                finalPrompt,
                chunk -> {
                    System.out.print(chunk);
                    accumulated.updateAndGet(prev -> prev + chunk);
                },
                () -> {
                    long elapsed = System.currentTimeMillis() - start;
                    System.out.println("\n----------------------------------------");
                    System.out.println("[DONE] 用时 " + elapsed + "ms / 共 " + accumulated.get().length() + " 字");
                    System.exit(0);
                },
                err -> {
                    error.set(err);
                    System.err.println("\n[ERROR] " + err.getClass().getSimpleName() + ": " + err.getMessage());
                    System.exit(2);
                }
        );

        // 兜底超时
        try {
            Thread.sleep(125_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.err.println("[TIMEOUT] SparkClient 125s 内未返回，强制退出");
        System.exit(3);
    }

    private static String readPromptFromStdin() {
        System.out.print("请输入 prompt（直接回车使用默认）：");
        try (Scanner sc = new Scanner(System.in)) {
            String line = sc.nextLine().trim();
            return line.isEmpty() ? "你好，请做一下自我介绍" : line;
        }
    }
}