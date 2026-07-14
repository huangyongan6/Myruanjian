package com.learngen.ai;

import com.learngen.exception.AIServiceException;

import java.util.function.Consumer;

/**
 * 讯飞星火 API 客户端抽象。
 *
 * <p>对应 CLAUDE.md §19 / §12：所有讯飞 API 调用统一通过此入口。
 * 实现类负责鉴权、SSE 流式解析、错误处理。
 */
public interface SparkClient {

    /**
     * 流式对话调用。
     *
     * <p>边接收边通过回调推送片段。CLAUDE.md §19：连接超时 10s、读取超时 60s，
     * 必须捕获网络异常并包装为 {@link AIServiceException}。
     *
     * @param systemPrompt 系统提示词（Agent 角色 + 输出格式约束）
     * @param userInput    用户输入
     * @param onChunk      每片内容回调（非 null，可能被调用多次）
     * @param onDone       结束回调（成功完成时调用一次）
     * @param onError      异常回调（任何异常出现时调用一次）
     */
    void streamChat(String systemPrompt,
                    String userInput,
                    Consumer<String> onChunk,
                    Runnable onDone,
                    Consumer<Throwable> onError);
}