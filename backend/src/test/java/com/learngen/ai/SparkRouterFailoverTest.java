package com.learngen.ai;

import com.learngen.exception.AIServiceException;
import com.learngen.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 主备路由器 SparkRouter 单测（CLAUDE.md §20.1 主备切换策略）。
 *
 * <p>使用 Mockito mock 两个 {@link SparkClient}（primary / secondary）；通过路径验证：
 * <ol>
 *   <li>限流/鉴权/IO 错误 → 切 secondary</li>
 *   <li>内容违规/长度超限（BusinessException）→ 不切</li>
 *   <li>secondary=null → 降级为单 key</li>
 *   <li>secondary 也失败 → 透传 secondary 错误，不循环</li>
 *   <li>熔断器打开后不尝试 secondary</li>
 * </ol>
 *
 * <p>测试不依赖 OkHttp，不启动 Spring 容器。
 */
class SparkRouterFailoverTest {

    private SparkClient primary;
    private SparkClient secondary;

    @BeforeEach
    void setUp() {
        primary = mock(SparkClient.class);
        secondary = mock(SparkClient.class);
    }

    // ===== 辅助方法 =====

    /** 让 primary 在流式流的第一个 chunk 里触发一个 onError（模拟 rate-limit）。 */
    private static void stubPrimaryChunkError(SparkClient primary, int errorCode, String errorMsg) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = invocation.getArgument(4);
            onError.accept(new AIServiceException(errorMsg + " code=" + errorCode));
            return null;
        }).when(primary).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /** 让 primary 在流式流的第一个 chunk 里触发一个 BusinessException。 */
    private static void stubPrimaryBusinessError(SparkClient primary, int code) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = invocation.getArgument(4);
            onError.accept(new BusinessException(400,
                    "内容被讯飞过滤或长度超限 code=" + code + ": the_message"));
            return null;
        }).when(primary).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /** 让 primary 抛 IOException（在 streamChat 开头即 fire onError，不经过 SSE）。 */
    private static void stubPrimaryIOException(SparkClient primary) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = invocation.getArgument(4);
            onError.accept(new AIServiceException("讯飞网络异常：connect timeout"));
            return null;
        }).when(primary).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /** 让 secondary 正常推 chunks 并 onDone。 */
    private static void stubSecondarySuccess(SparkClient secondary, List<String> chunks) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onDone = invocation.getArgument(3);
            for (String c : chunks) {
                onChunk.accept(c);
            }
            onDone.run();
            return null;
        }).when(secondary).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /** 让 secondary 走 onError 路径。 */
    private static void stubSecondaryError(SparkClient secondary, Throwable err) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Throwable> onError = invocation.getArgument(4);
            onError.accept(err);
            return null;
        }).when(secondary).streamChat(anyString(), anyString(), any(), any(), any());
    }

    // ===== 测试用例 =====

    /**
     * 1. primary 限流 (rate-limit, code=10110) → 切 secondary → secondary 正常输出。
     */
    @Test
    void primaryRateLimit_routesToSecondary_succeeds() throws InterruptedException {
        stubPrimaryChunkError(primary, 10110, "讯飞限流或服务繁忙");
        stubSecondarySuccess(secondary, List.of("二", "次", "尝试"));

        SparkRouter router = new SparkRouter(primary, secondary, 3, 60);
        List<String> received = new ArrayList<>();
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean errored = new AtomicBoolean(false);

        router.streamChat("sys", "hello",
                received::add,
                doneLatch::countDown,
                err -> errored.set(true));

        assertTrue(doneLatch.await(2, TimeUnit.SECONDS), "onDone 未触发");
        assertFalse(errored.get(), "不应走 onError");
        assertEquals(List.of("二", "次", "尝试"), received);

        // primary 被调用一次，secondary 被调用一次
        verify(primary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
        verify(secondary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /**
     * 2. primary 内容违规 (code=10013, BusinessException) → 不切 secondary → 透传异常。
     */
    @Test
    void primaryContentError_doesNotRouteToSecondary() throws InterruptedException {
        stubPrimaryBusinessError(primary, 10013);

        SparkRouter router = new SparkRouter(primary, secondary, 3, 60);
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> caught = new AtomicReference<>();

        router.streamChat("sys", "hello",
                chunk -> { },
                () -> { },
                err -> { caught.set(err); errorLatch.countDown(); });

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(caught.get());
        assertTrue(caught.get() instanceof BusinessException, "应为 BusinessException，实为 " + caught.get().getClass().getSimpleName());

        verify(primary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
        verify(secondary, never()).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /**
     * 3. secondary=null → 只走 primary，primary 限流 → onError 透传不 NPE。
     */
    @Test
    void secondaryNotConfigured_primaryRateLimit_propagatesPrimaryError() throws InterruptedException {
        stubPrimaryChunkError(primary, 10110, "讯飞限流或服务繁忙");
        SparkRouter router = new SparkRouter(primary, null, 3, 60);

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> caught = new AtomicReference<>();

        router.streamChat("sys", "hello",
                chunk -> { },
                () -> { },
                err -> { caught.set(err); errorLatch.countDown(); });

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(caught.get());
        assertTrue(caught.get() instanceof AIServiceException, "应为 AIServiceException");
        assertTrue(caught.get().getMessage().contains("限流"));

        verify(primary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /**
     * 4. primary IO 异常 → 切 secondary → secondary 正常。
     */
    @Test
    void primaryIOException_routesToSecondary() throws InterruptedException {
        stubPrimaryIOException(primary);
        stubSecondarySuccess(secondary, List.of("ok"));

        SparkRouter router = new SparkRouter(primary, secondary, 3, 60);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean errored = new AtomicBoolean(false);

        router.streamChat("sys", "hello",
                chunk -> { },
                doneLatch::countDown,
                err -> errored.set(true));

        assertTrue(doneLatch.await(2, TimeUnit.SECONDS));
        assertFalse(errored.get());

        verify(primary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
        verify(secondary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
    }

    /**
     * 5. primary 限流 → secondary 也失败 → 透传 secondary 错误（不循环回 primary）。
     */
    @Test
    void secondaryAlsoFails_propagatesSecondaryError() throws InterruptedException {
        stubPrimaryChunkError(primary, 10110, "讯飞限流或服务繁忙");
        stubSecondaryError(secondary, new AIServiceException("讯飞鉴权错误 code=11200"));

        SparkRouter router = new SparkRouter(primary, secondary, 3, 60);
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> caught = new AtomicReference<>();

        router.streamChat("sys", "hello",
                chunk -> { },
                () -> { },
                err -> { caught.set(err); errorLatch.countDown(); });

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(caught.get());
        assertTrue(caught.get().getMessage().contains("鉴权"),
                "应把 secondary 的错误透传，实际消息：" + caught.get().getMessage());

        verify(primary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
        verify(secondary, times(1)).streamChat(anyString(), anyString(), any(), any(), any());
        // 注意：verify primary 只调一次即可——不循环
    }

    /**
     * 6. 熔断器：secondary 连续失败 3 次后冷却，第 4 次 primary 命中可重试错误时不切。
     */
    @Test
    void consecutiveSecondaryFailures_circuitBreakerOpens() throws InterruptedException {
        stubPrimaryChunkError(primary, 10110, "讯飞限流或服务繁忙 code=10110");
        stubSecondaryError(secondary, new AIServiceException("讯飞业务错误 code=10999"));

        // 用固定时钟，每次调用 advance
        AtomicLong fakeClock = new AtomicLong(0);
        SparkRouter router = new SparkRouter(primary, secondary, 3, 5000L, fakeClock::get);

        CountDownLatch errorLatch1 = new CountDownLatch(1);
        router.streamChat("s", "1", c -> { }, () -> { }, e -> errorLatch1.countDown());
        assertTrue(errorLatch1.await(2, TimeUnit.SECONDS));
        assertEquals(1, router.getSecondaryConsecutiveFailures());

        CountDownLatch errorLatch2 = new CountDownLatch(1);
        router.streamChat("s", "2", c -> { }, () -> { }, e -> errorLatch2.countDown());
        assertTrue(errorLatch2.await(2, TimeUnit.SECONDS));
        assertEquals(2, router.getSecondaryConsecutiveFailures());

        CountDownLatch errorLatch3 = new CountDownLatch(1);
        router.streamChat("s", "3", c -> { }, () -> { }, e -> errorLatch3.countDown());
        assertTrue(errorLatch3.await(2, TimeUnit.SECONDS));
        assertEquals(3, router.getSecondaryConsecutiveFailures());
        assertTrue(router.getSecondaryDisabledUntilMs() > 0, "熔断应该打开（disabledUntil > 0）");

        // 第 4 次 → primary 再触发限流，但此时 secondary disabled，应直接透传 primary 的 error
        // 改 stub：primary 第 4 次触发非可重试错误（但 Router 先判断 shouldSwitch → disabledUntil → 跳过）
        // primary 第 4 次限流：Router 里 shouldSwitch d=RETRYABLE_RATELIMIT → 检查 disabledUntil → now(0) < disabledUntil(recent) → 不切
        CountDownLatch errorLatch4 = new CountDownLatch(1);
        AtomicReference<Throwable> caught4 = new AtomicReference<>();
        router.streamChat("s", "4", c -> { }, () -> { }, e -> { caught4.set(e); errorLatch4.countDown(); });
        assertTrue(errorLatch4.await(2, TimeUnit.SECONDS));
        assertNotNull(caught4.get());
        assertTrue(caught4.get().getMessage().contains("限流"), "应透传 primary 的限流错误，实际：" + caught4.get().getMessage());

        // secondary 在冷却期不被调用 → primary 4 次，secondary 3 次
        verify(primary, times(4)).streamChat(anyString(), anyString(), any(), any(), any());
        verify(secondary, times(3)).streamChat(anyString(), anyString(), any(), any(), any());
    }
}
