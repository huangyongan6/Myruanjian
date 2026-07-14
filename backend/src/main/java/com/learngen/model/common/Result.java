package com.learngen.model.common;

import lombok.Data;

/**
 * 统一 REST 响应体。
 *
 * <p>对应 CLAUDE.md §8.1。Controller 方法返回值必须包裹为 {@code Result<T>}。
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> {

    /** 状态码：200 成功；400 参数错误；401 未认证；404 资源不存在；500 服务器错误 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 数据体 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据）。 */
    public static <T> Result<T> success() {
        return new Result<>(200, "ok", null);
    }

    /** 成功（带数据）。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "ok", data);
    }

    /** 自定义错误。 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}