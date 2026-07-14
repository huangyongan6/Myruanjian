package com.learngen.service;

import com.learngen.model.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 对话服务接口。
 *
 * <p>对应 CLAUDE.md §4.1 ChatService。
 */
public interface ChatService {

    /**
     * 处理一条用户消息：意图识别 → 路由 Agent → 持久化对话 → 返回 Agent 输出。
     *
     * <p>同步入口（REST {@code /api/chat/send} 使用）。内部委托
     * {@link #handleMessageStream} + 同步等待，向后兼容。
     *
     * @param studentId 学生 ID
     * @param userInput 用户输入文本
     * @return Agent 输出的 payload（字符串）
     */
    String handleMessage(Long studentId, String userInput);

    /**
     * 流式处理一条用户消息（WebSocket {@code /app/chat.send} 使用）。
     *
     * <p>对应 CLAUDE.md §22 WebSocket 流式规范：每收到一个 chunk 立即触发
     * {@code onChunk}；正常完成触发 {@code onDone}；异常触发 {@code onError}。
     * {@code onDone} 与 {@code onError} 互斥。
     *
     * <p>对话持久化策略：user 消息在入口处立即写入；assistant 消息仅在
     * {@code onDone} 时一次性写入完整 payload，避免循环 SQL（CLAUDE.md §15）。
     * 异常分支也会写入一条 assistant 行（payload = 错误摘要），保证历史完整。
     *
     * @param studentId 学生 ID
     * @param userInput 用户输入文本
     * @param onChunk   每片内容回调（可能被调用 0..N 次）
     * @param onDone    结束回调
     * @param onError   异常回调
     */
    void handleMessageStream(Long studentId,
                             String userInput,
                             Consumer<String> onChunk,
                             Runnable onDone,
                             Consumer<Throwable> onError);

    /**
     * 查询某学生的对话历史（按时间倒序）。
     *
     * <p>支持基于 {@code before} 的游标分页：当传入 {@code before}（ISO-8601 时间字符串）
     * 时，仅返回 createdAt 严格早于该时间的消息，便于前端向上滚动加载更早的历史。
     *
     * @param studentId 学生 ID
     * @param limit     最多返回条数（默认 50，上限 200）
     * @param before    可选游标；为 null 时取最新 limit 条
     */
    List<ChatMessage> history(Long studentId, int limit, String before);
}