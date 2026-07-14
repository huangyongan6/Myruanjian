package com.learngen.ai;

import com.learngen.exception.AIServiceException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * 讯飞星火主备路由器（CLAUDE.md §20.1）。
 *
 * <p>对外实现 {@link SparkClient}。路由策略：
 * <ol>
 *   <li>优先调用 primary。</li>
 *   <li>当 primary 命中可重试错误（限流 / 鉴权 / 网络 IO）且 secondary 已配置时，
 *       切换到 secondary 重发同一请求——每个请求最多切一次。</li>
 *   <li>业务错误（{@link BusinessException}，包括内容违规、长度超限等）一律不切，
 *       直接透传 primary 的异常。</li>
 *   <li>secondary 也失败时，透传 secondary 的异常，绝不回切 primary。</li>
 *   <li>secondary 未配置时降级为只走 primary。</li>
 * </ol>
 *
 * <p>轻量熔断：secondary 连续失败达到 {@code failureThreshold} 次后冷却 {@code cooldownMs} 毫秒。
 * 冷却期间 primary 即便命中可重试条件也只走 primary、记 warn。
 *
 * <p>决策依据：不依赖 SparkClientImpl 内部 hook——在 primary 的 {@code onError} 回调中，
 * 依据 Throwable 类型和消息反推 {@link SwitchDecision}：
 * <ul>
 *   <li>{@code BusinessException}（内容违规码 10013/10014/10019/10907）→ NON_RETRYABLE</li>
 *   <li>{@code AIServiceException} 含 "限流"/"容量" → RETRYABLE_RATELIMIT</li>
 *   <li>{@code AIServiceException} 含 "APIPassword 无效"/"版本不匹配"/"未配置" → RETRYABLE_AUTH</li>
 *   <li>{@code AIServiceException} 含 "网络异常"/"HTTP"/"响应体为空" → RETRYABLE_IO</li>
 *   <li>其他 → RETRYABLE_IO（偏激进：未知网络类异常也切，secondary 救不活则返回其错误）</li>
 * </ul>
 */
@Slf4j
public class SparkRouter implements SparkClient {

    private final SparkClient primary;
    /** 允许为 {@code null}：未配置时降级为单 key 行为。 */
    private final SparkClient secondary;
    private final int failureThreshold;
    private final long cooldownMs;
    private final LongSupplier clock;

    private volatile long secondaryConsecutiveFailures;
    private volatile long secondaryDisabledUntilMs;

    public SparkRouter(SparkClient primary, SparkClient secondary,
                       int failureThreshold, long cooldownSeconds) {
        this(primary, secondary, failureThreshold, cooldownSeconds * 1000L, System::currentTimeMillis);
    }

    /** 测试用：注入时钟。 */
    SparkRouter(SparkClient primary, SparkClient secondary,
                int failureThreshold, long cooldownMs, LongSupplier clock) {
        this.primary = primary;
        this.secondary = secondary;
        this.failureThreshold = failureThreshold;
        this.cooldownMs = cooldownMs;
        this.clock = clock;
        this.secondaryConsecutiveFailures = 0;
        this.secondaryDisabledUntilMs = 0;
        log.info("SparkRouter 初始化：primary={}, secondary={}, 主备切换={}, 熔断阈值={}/冷却={}ms",
                primary != null ? "已配置" : "未配置",
                secondary != null ? "已配置" : "未配置",
                isFailoverEnabled() ? "启用" : "降级为单 key",
                failureThreshold, cooldownMs);
    }

    public boolean isFailoverEnabled() {
        return secondary != null;
    }

    @Override
    public void streamChat(String systemPrompt,
                           String userInput,
                           Consumer<String> onChunk,
                           Runnable onDone,
                           Consumer<Throwable> onError) {
        if (primary == null) {
            onError.accept(new AIServiceException("SparkRouter: primary 未配置"));
            return;
        }

        // 防重放：保证 chunkOnce 不会被 onDone/onError 抢跑后重复 fire
        // （正常路径：onChunk 多次 → onDone；异常路径：onChunk 零次或多次 → onError）
        boolean[] callerDoneOrError = {false};
        Consumer<String> chunkOnce = chunk -> {
            if (callerDoneOrError[0]) return;
            onChunk.accept(chunk);
        };
        Runnable doneOnce = () -> {
            if (callerDoneOrError[0]) return;
            callerDoneOrError[0] = true;
            onDone.run();
        };
        Consumer<Throwable> errorOnce = err -> {
            if (callerDoneOrError[0]) return;
            callerDoneOrError[0] = true;
            onError.accept(err);
        };

        // 切到 secondary 最多一次
        boolean[] switched = {false};

        Consumer<Throwable> primaryErrorHook = err -> {
            SwitchDecision d = SwitchDecision.classify(err);
            log.debug("primary 异常 classified as {}, secondaryEnabled={} switched={}",
                    d, isFailoverEnabled(), switched[0]);

            if (shouldSwitch(d) && !switched[0]) {
                long disabledUntil = secondaryDisabledUntilMs;
                long now = clock.getAsLong();
                if (now < disabledUntil) {
                    log.warn("secondary 处于冷却期(剩余{}ms)，本次不切，透传 primary 错误",
                            disabledUntil - now);
                    errorOnce.accept(err);
                    return;
                }
                switched[0] = true;
                log.warn("primary 命中可重试错误 class={}，切换到 secondary", d);
                invokeSecondary(systemPrompt, userInput, chunkOnce, doneOnce, errorOnce);
                return;
            }
            // 不切：透传 primary 的异常
            errorOnce.accept(err);
        };

        primary.streamChat(systemPrompt, userInput, chunkOnce, doneOnce, primaryErrorHook);
    }

    /**
     * 调用 secondary 并处理结果。
     * secondary 成功 → 重置熔断计数；secondary 失败 → 计数 + 判断熔断 + 透传。
     */
    private void invokeSecondary(String systemPrompt, String userInput,
                                 Consumer<String> chunkOnce, Runnable doneOnce,
                                 Consumer<Throwable> errorOnce) {
        secondary.streamChat(
                systemPrompt, userInput,
                chunkOnce,
                () -> {
                    synchronized (this) {
                        secondaryConsecutiveFailures = 0;
                    }
                    log.info("secondary 调用成功，熔断计数器已重置");
                    doneOnce.run();
                },
                err -> {
                    long failures;
                    synchronized (this) {
                        failures = ++secondaryConsecutiveFailures;
                    }
                    log.error("secondary 失败（连续{}次）class={} msg={}",
                            failures, SwitchDecision.classify(err), err.getMessage());
                    if (failures >= failureThreshold) {
                        long until = clock.getAsLong() + cooldownMs;
                        secondaryDisabledUntilMs = until;
                        log.warn("secondary 连续失败 {} 次（阈值={}），冷却 {}ms 至 {}",
                                failures, failureThreshold, cooldownMs, until);
                    }
                    errorOnce.accept(err);
                });
    }

    private boolean shouldSwitch(SwitchDecision d) {
        if (!isFailoverEnabled()) return false;
        return d == SwitchDecision.RETRYABLE_AUTH
                || d == SwitchDecision.RETRYABLE_RATELIMIT
                || d == SwitchDecision.RETRYABLE_IO;
    }

    // ========== 测试用 ==========

    long getSecondaryConsecutiveFailures() {
        return secondaryConsecutiveFailures;
    }

    long getSecondaryDisabledUntilMs() {
        return secondaryDisabledUntilMs;
    }
}
