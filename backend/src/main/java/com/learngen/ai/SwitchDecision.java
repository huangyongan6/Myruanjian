package com.learngen.ai;

import com.learngen.exception.AIServiceException;
import com.learngen.exception.BusinessException;

/**
 * 主备切换决策（CLAUDE.md §20.1：SparkRouter 内部使用）。
 *
 * <p>决策依据来自 {@link SparkClientImpl} 抛出的 Throwable 类型及其消息内容。
 * 枚举值本身不编码具体的讯飞错误码——由 {@link #classify(Throwable)} 统一派生。
 */
public enum SwitchDecision {
    /** 限流/容量错误（code 10110、11201-11203）—— 应当切。 */
    RETRYABLE_RATELIMIT,
    /** 鉴权错误（code 10015、10016、11200）—— 应当切。 */
    RETRYABLE_AUTH,
    /** 网络/IO 异常 — 应当切。 */
    RETRYABLE_IO,
    /** 业务错误（{@link BusinessException}：内容/长度违规等）—— 不切。 */
    NON_RETRYABLE;

    /**
     * 从 Throwable 反推决策。
     *
     * <p>对应 {@link SparkClientImpl#handleServerError(int, String, java.util.function.Consumer)}
     * 的三条分支和 IOException 反推的具体消息文本。
     */
    public static SwitchDecision classify(Throwable err) {
        if (err instanceof BusinessException) {
            return NON_RETRYABLE;
        }
        if (err instanceof AIServiceException) {
            String msg = err.getMessage() == null ? "" : err.getMessage();
            if (msg.contains("限流") || msg.contains("容量") || msg.contains("服务繁忙")) {
                return RETRYABLE_RATELIMIT;
            }
            if (msg.contains("APIPassword 无效") || msg.contains("版本不匹配")
                    || msg.contains("APIPassword 未配置")) {
                return RETRYABLE_AUTH;
            }
            if (msg.contains("网络异常") || msg.contains("HTTP") || msg.contains("响应体为空")
                    || msg.contains("构造讯飞请求体失败")) {
                return RETRYABLE_IO;
            }
        }
        // 兜底：未知异常偏激进试 secondary
        return RETRYABLE_IO;
    }
}
