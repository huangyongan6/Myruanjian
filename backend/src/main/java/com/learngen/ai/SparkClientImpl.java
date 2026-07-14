package com.learngen.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learngen.exception.AIServiceException;
import com.learngen.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 讯飞星火 Spark HTTP 客户端实现（OpenAI 兼容 schema + SSE 流式）。
 *
 * <p>对应 CLAUDE.md §19 / §12.1：
 * <ul>
 *   <li>鉴权：HTTP Header {@code Authorization: Bearer <APIPassword>}</li>
 *   <li>请求体：OpenAI 兼容 schema（model / messages / stream）</li>
 *   <li>响应：SSE（{@code data: {...}}），每片取 {@code choices[0].delta.content}</li>
 *   <li>结束信号：{@code data: [DONE]} 或最后一片含 {@code usage}</li>
 *   <li>超时：连接 10s / 读取 60s / 单次请求 120s（避免线程挂死）</li>
 *   <li>异常：网络/超时 → AIServiceException；内容/限流 → 区分 AIServiceException 与 BusinessException</li>
 * </ul>
 *
 * <p>本类不是 {@code @Component}——由 {@link SparkClientConfig} 显式注册为两个具名 Bean（primary/secondary）。
 * CLI 工具可显式 {@code new} 一个临时实例用于连通性验证。
 *
 * <p>主备切换决策由 {@link SparkRouter} 依据本类抛出的 Throwable 类型及消息内容反推。
 */
@Slf4j
public class SparkClientImpl implements SparkClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DONE_MARKER = "[DONE]";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String host;
    private final String apiPath;
    private final String apiPassword;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final int totalTimeoutSeconds;

    /**
     * 唯一构造器。Spring 容器中由 {@link SparkClientConfig} 显式构造；
     * CLI / 单测可显式 {@code new} 一个临时实例。
     */
    public SparkClientImpl(ObjectMapper objectMapper, String host, String apiPath, String apiPassword,
                           String model, double temperature, int maxTokens,
                           int connectTimeout, int readTimeout, int totalTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.host = host;
        this.apiPath = apiPath;
        this.apiPassword = apiPassword;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.totalTimeoutSeconds = totalTimeoutSeconds;
        // CLAUDE.md §19：连接超时 10s，读取超时 60s
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(connectTimeout, TimeUnit.SECONDS)
                .callTimeout(totalTimeoutSeconds, TimeUnit.SECONDS)
                .build();
        log.info("SparkClient 初始化完成 url=https://{}{} model={} apiPasswordConfigured={}",
                host, apiPath, model, isApiPasswordConfigured());
    }

    /** 调用前检查 APIPassword 是否配置（CLAUDE.md §19）。 */
    public boolean isApiPasswordConfigured() {
        return apiPassword != null
                && !apiPassword.isBlank()
                && !"please-set-via-env".equals(apiPassword);
    }

    @Override
    public void streamChat(String systemPrompt,
                           String userInput,
                           Consumer<String> onChunk,
                           Runnable onDone,
                           Consumer<Throwable> onError) {
        if (!isApiPasswordConfigured()) {
            onError.accept(new AIServiceException(
                    "讯飞 APIPassword 未配置，请在环境变量 SPARK_API_PASSWORD_PRIMARY/SECONDARY 中设置"));
            return;
        }
        if (userInput == null || userInput.isBlank()) {
            onError.accept(new BusinessException(400, "用户输入不能为空"));
            return;
        }

        // 1. 构造请求体
        String requestBody;
        try {
            requestBody = buildRequestBody(systemPrompt, userInput);
        } catch (Exception e) {
            onError.accept(new AIServiceException("构造讯飞请求体失败", e));
            return;
        }

        String url = "https://" + host + apiPath;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiPassword)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        // 2. 发起请求并按行读 SSE
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String msg = response.message();
                log.error("讯飞请求 HTTP 失败 code={} msg={}", code, msg);
                onError.accept(new AIServiceException(
                        "讯飞请求失败 HTTP " + code + " " + msg));
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                onError.accept(new AIServiceException("讯飞响应体为空"));
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    // SSE 帧以 "data:" 开头；空行表示一个事件结束
                    if (line.isEmpty() || !line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (DONE_MARKER.equals(payload)) {
                        break;
                    }

                    String content = extractDeltaContent(payload);
                    if (content != null && !content.isEmpty()) {
                        try {
                            onChunk.accept(content);
                        } catch (Exception consumerErr) {
                            log.warn("onChunk 回调异常：{}", consumerErr.getMessage());
                        }
                    }

                    // 错误码检查（每个 chunk 都可能含 code 字段）
                    Integer code = extractTopLevelCode(payload);
                    if (code != null && code != 0) {
                        String errMsg = extractTopLevelMessage(payload);
                        handleServerError(code, errMsg, onError);
                        return;
                    }

                    // usage 非空也可作为流结束信号
                    if (extractUsageTotal(payload) != null) {
                        log.debug("讯飞流 usage 已返回，继续读取直到 [DONE]");
                    }
                }
            }

            onDone.run();
        } catch (IOException e) {
            log.error("讯飞网络异常：{}", e.getMessage(), e);
            onError.accept(new AIServiceException("讯飞网络异常：" + e.getMessage(), e));
        } catch (Exception e) {
            log.error("讯飞调用未知异常：{}", e.getMessage(), e);
            onError.accept(new AIServiceException("讯飞调用异常：" + e.getMessage(), e));
        }
    }

    /** 构造 OpenAI 兼容的请求体。 */
    private String buildRequestBody(String systemPrompt, String userInput) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", true);
        root.put("temperature", temperature);
        root.put("max_tokens", maxTokens);
        root.put("user", "learngen-user");

        ArrayNode messages = objectMapper.createArrayNode();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userInput);
        messages.add(userMsg);

        root.set("messages", messages);
        return objectMapper.writeValueAsString(root);
    }

    /**
     * 从 SSE chunk 中提取文本片段（路径：{@code choices[0].delta.content}）。
     *
     * @return 文本片段；字段不存在或为空时返回 {@code null}
     */
    private String extractDeltaContent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode choices = node.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                return null;
            }
            JsonNode content = delta.get("content");
            return content == null ? null : content.asText();
        } catch (Exception e) {
            log.debug("解析 chunk 失败：{} | payload={}", e.getMessage(),
                    json.length() > 200 ? json.substring(0, 200) + "..." : json);
            return null;
        }
    }

    /** 提取顶层 code 字段；不存在时返回 null。 */
    private Integer extractTopLevelCode(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode code = node.get("code");
            return code == null || code.isNull() ? null : code.asInt();
        } catch (Exception e) {
            return null;
        }
    }

    /** 提取顶层 message 字段。 */
    private String extractTopLevelMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode msg = node.get("message");
            return msg == null ? null : msg.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /** 提取 usage.total_tokens；不存在时返回 null。 */
    private Integer extractUsageTotal(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode usage = node.get("usage");
            if (usage == null) return null;
            JsonNode total = usage.get("total_tokens");
            return total == null ? null : total.asInt();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按错误码分类抛出：限流/容量 → AIServiceException；内容/长度 → BusinessException；
     * 其他非 0 → AIServiceException。
     */
    private void handleServerError(int code, String msg, Consumer<Throwable> onError) {
        String text = msg == null ? "" : msg;
        // 内容违规 / 长度超限：业务层处理
        if (code == 10013 || code == 10014 || code == 10019 || code == 10907) {
            log.warn("讯飞内容/长度相关错误 code={} msg={}", code, text);
            onError.accept(new BusinessException(400,
                    "内容被讯飞过滤或长度超限 code=" + code + ": " + text));
            return;
        }
        // 限流 / 容量：AI 服务异常，可重试
        if (code == 10110 || (code >= 11201 && code <= 11203)) {
            log.error("讯飞限流/容量错误 code={} msg={}", code, text);
            onError.accept(new AIServiceException(
                    "讯飞限流或服务繁忙 code=" + code + ": " + text));
            return;
        }
        // 鉴权：APIPassword 错误或版本不匹配
        if (code == 10015 || code == 10016 || code == 11200) {
            log.error("讯飞鉴权错误 code={} msg={}", code, text);
            onError.accept(new AIServiceException(
                    "讯飞 APIPassword 无效或版本不匹配 code=" + code + ": " + text));
            return;
        }
        // 其他
        log.error("讯飞业务错误 code={} msg={}", code, text);
        onError.accept(new AIServiceException(
                "讯飞业务错误 code=" + code + ": " + text));
    }
}
