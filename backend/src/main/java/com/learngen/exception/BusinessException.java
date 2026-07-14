package com.learngen.exception;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>对应 CLAUDE.md §7.1。可预期的业务错误，如「画像不存在」「参数非法」。
 * 日志级别 {@code warn}。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码（建议使用 4xx 段，便于前端按码定位） */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}