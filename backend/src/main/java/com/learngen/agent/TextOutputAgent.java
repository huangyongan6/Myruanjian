package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.SparkClient;
import com.learngen.exception.AIServiceException;
import com.learngen.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 输出为文本/JSON 的 Agent 抽象基类。
 *
 * <p>对应 CLAUDE.md §4.3 中 DocAgent / QuizAgent / ReadingAgent 等输出文本的 Agent。
 * 统一处理：流式回调拼装 → 同步等待 → 标准化输出（去除 Markdown 包裹）。
 *
 * <p>同时实现 {@link StreamableAgent}，提供 {@link #processStream} 直接把
 * SparkClient 的 chunk 回调透传给上层（用于 WebSocket 逐条推送）。
 * {@link #process} 内部复用 {@code processStream} + 同步等待 + 异常上抛，保持向后兼容。
 */
@Slf4j
public abstract class TextOutputAgent extends AgentBase implements StreamableAgent {

    protected final SparkClient sparkClient;
    protected final ObjectMapper objectMapper;

    protected TextOutputAgent(String name, String role, String agentType,
                              SparkClient sparkClient, ObjectMapper objectMapper) {
        super(name, role, agentType);
        this.sparkClient = sparkClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 子类提供 System Prompt。
     */
    protected abstract String systemPrompt();

    /**
     * 同步 process：内部委托 {@link #processStream}，用 {@link CountDownLatch}
     * 拼完整字符串后调用 {@link #normalize}。上游异常按 linter 修订版上抛，
     * 让 {@code GlobalExceptionHandler} 统一处理（业务/AI 异常区分）。
     */
    @Override
    public AgentMessage process(AgentMessage input) {
        String userInput = buildUserInput(input);
        if (userInput.isBlank()) {
            log.warn("{} 输入为空，上抛 BusinessException", name);
            throw new BusinessException(400, "知识点不能为空，无法生成资源");
        }

        StringBuilder accumulated = new StringBuilder();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        processStream(input, accumulated::append, latch::countDown, err -> {
            errorRef.set(err);
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("{} 等待 SparkClient 被中断", name);
            throw new AIServiceException(name + " 被中断，请稍后再试");
        }

        if (errorRef.get() != null) {
            Throwable err = errorRef.get();
            log.warn("{} 调用失败，上抛异常：{}", name, err.getMessage());
            if (err instanceof BusinessException be) {
                throw be;
            }
            if (err instanceof RuntimeException re) {
                throw re;
            }
            // 检查型异常：包成 AIServiceException 上抛
            throw new AIServiceException(name + " 调用失败：" + err.getMessage(), err);
        }

        String payload = normalize(accumulated.toString());
        if (payload.isBlank()) {
            log.warn("{} 返回空 payload（上游无内容），上抛 AIServiceException", name);
            throw new AIServiceException(name + " 未生成任何内容，请稍后再试");
        }
        log.info("{} 生成完成 payload 长度={}", name, payload.length());

        return AgentMessage.builder()
                .agentName(name)
                .role("assistant")
                .payload(payload)
                .studentId(input.getStudentId())
                .build();
    }

    /**
     * 流式 process：把 SparkClient 的 chunk 回调透传给上层，自身不阻塞。
     *
     * <p>对应 CLAUDE.md §22 WebSocket 流式规范。每收到一个 SSE delta
     * 立即触发一次 {@code onChunk}；流结束时触发 {@code onDone}；异常触发 {@code onError}。
     *
     * <p>回调内部各自 try-catch：单条 chunk / 一次 onError / 一次 onDone 抛异常
     * 都不会阻断 SparkClient 后续读取，与 {@link SparkClientImpl} 容错风格一致。
     */
    @Override
    public void processStream(AgentMessage input,
                              Consumer<String> onChunk,
                              Runnable onDone,
                              Consumer<Throwable> onError) {
        String userInput = buildUserInput(input);
        if (userInput.isBlank()) {
            log.warn("{} 输入为空，跳过流式调用", name);
            safeRun(onDone);
            return;
        }

        Consumer<String> safeChunk = chunk -> {
            if (chunk == null || chunk.isEmpty()) return;
            try {
                onChunk.accept(chunk);
            } catch (Exception e) {
                log.warn("{} onChunk 回调异常：{}", name, e.getMessage());
            }
        };
        Consumer<Throwable> safeError = err -> {
            try {
                onError.accept(err);
            } catch (Exception e) {
                log.warn("{} onError 回调异常：{}", name, e.getMessage());
            }
        };
        Runnable safeDone = () -> safeRun(onDone);

        sparkClient.streamChat(systemPrompt(), userInput, safeChunk, safeDone, safeError);
    }

    private void safeRun(Runnable r) {
        if (r == null) return;
        try {
            r.run();
        } catch (Exception e) {
            log.warn("{} onDone 回调异常：{}", name, e.getMessage());
        }
    }

    /**
     * 子类可重写：拼接上下文（知识点、难度、画像、知识库摘要）成 user input。
     */
    protected String buildUserInput(AgentMessage input) {
        StringBuilder sb = new StringBuilder();
        if (input.getKnowledgePoint() != null) {
            sb.append("知识点：").append(input.getKnowledgePoint()).append('\n');
        }
        if (input.getDifficulty() != null) {
            sb.append("难度：").append(input.getDifficulty()).append('\n');
        }
        // CLAUDE.md §19 防幻觉：注入知识库 Markdown 摘要作为事实参考
        if (input.getContext() != null) {
            Object preview = input.getContext().get("knowledge_preview");
            if (preview instanceof String s && !s.isBlank()) {
                sb.append("【知识库参考】（请基于以下事实回答，避免捏造）\n")
                  .append(s).append("\n\n");
            }
        }
        if (input.getContent() != null) {
            sb.append("上下文：").append(input.getContent());
        }
        return sb.toString().trim();
    }

    /**
     * 标准化输出：去除 Markdown 代码块包裹。
     */
    protected String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultEmptyPayload();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    /** 空输出的占位 payload，子类按需重写。 */
    protected String defaultEmptyPayload() {
        return "";
    }
}