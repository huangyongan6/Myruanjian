package com.learngen.exception;

/**
 * AI 服务异常。
 *
 * <p>对应 CLAUDE.md §7.1。讯飞星火 API 调用失败、超时、返回异常、限流等场景。
 * 日志级别 {@code error}。
 */
public class AIServiceException extends RuntimeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}