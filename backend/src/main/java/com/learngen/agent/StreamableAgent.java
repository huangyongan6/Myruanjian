package com.learngen.agent;

import java.util.function.Consumer;

/**
 * 支持流式输出的 Agent 接口。
 *
 * <p>对应 CLAUDE.md §22 WebSocket 流式规范：实现此接口的 Agent 可逐 chunk
 * 推送内容到客户端，而非等全部生成完一次性返回。
 *
 * <p>约定：
 * <ul>
 *   <li>{@link #processStream} 在 AI 服务生成首个片段前**不应**阻塞调用线程，
 *       回调应在 SparkClient 推送时立即触发</li>
 *   <li>{@code onChunk} 可能被调用零次或多次；调用方负责容错（单次失败不影响后续）</li>
 *   <li>正常完成时调用一次 {@code onDone}；异常时调用一次 {@code onError}；
 *       二者**互斥**，最多触发其一</li>
 * </ul>
 *
 * <p>当前默认实现：{@link TextOutputAgent}。未来新增非文本输出型 Agent
 * （如 JSON 结构化资源）建议也实现本接口以获得流式体验。
 */
public interface StreamableAgent {

    /**
     * 流式处理输入消息。
     *
     * @param input    输入消息
     * @param onChunk  每片内容回调（非 null；可能多次调用）
     * @param onDone   结束回调（成功完成时调用一次）
     * @param onError  异常回调（任何错误时调用一次；与 onDone 互斥）
     */
    void processStream(AgentMessage input,
                       Consumer<String> onChunk,
                       Runnable onDone,
                       Consumer<Throwable> onError);
}