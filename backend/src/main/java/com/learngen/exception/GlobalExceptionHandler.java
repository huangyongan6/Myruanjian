package com.learngen.exception;

import com.learngen.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Pattern;

/**
 * 全局异常处理器。
 *
 * <p>对应 CLAUDE.md §7.1 / §9.3：Controller 层禁止 try-catch，统一由本类处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 匹配 "：id=数字" 或 ": id=数字"，用于脱敏内部详情 */
    private static final Pattern ID_DETAIL_PATTERN = Pattern.compile("[:：]\\s*(student)?id=\\d+", Pattern.CASE_INSENSITIVE);

    /**
     * 脱敏消息中的内部标识（studentId、id 等），避免泄露到前端。
     * 例如："学习路径不存在：studentId=2" → "学习路径不存在"
     */
    private static String sanitizeMessage(String message) {
        if (message == null) return "操作失败";
        String cleaned = ID_DETAIL_PATTERN.matcher(message).replaceAll("");
        return cleaned.isBlank() ? "操作失败" : cleaned.trim();
    }

    /** 业务异常：可预期，记 warn，返回脱敏后的友好提示。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        String raw = e.getMessage();
        log.warn("业务异常 code={} message={}", e.getCode(), raw);
        String friendly = sanitizeMessage(raw);
        return Result.error(e.getCode(), friendly);
    }

    /** AI 服务异常：调用讯飞 API 失败，记 error，统一友好提示。 */
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