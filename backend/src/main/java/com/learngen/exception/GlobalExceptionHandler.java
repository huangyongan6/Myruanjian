package com.learngen.exception;

import com.learngen.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>对应 CLAUDE.md §7.1 / §9.3：Controller 层禁止 try-catch，统一由本类处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期，记 warn，返回对应业务码。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** AI 服务异常：调用讯飞 API 失败，记 error，统一提示。 */
    @ExceptionHandler(AIServiceException.class)
    public Result<Void> handleAI(AIServiceException e) {
        log.error("AI 服务异常 message={}", e.getMessage(), e);
        return Result.error(500, "AI 服务暂时不可用，请稍后再试");
    }

    /** 兜底异常：未知运行时错误。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器内部错误");
    }
}