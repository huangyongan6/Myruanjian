package com.learngen.service.impl;

import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.exception.AIServiceException;
import com.learngen.mapper.ChatMessageMapper;
import com.learngen.model.ChatMessage;
import com.learngen.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流式对话 ChatService.handleMessageStream 测试。
 *
 * <p>对应计划文件 expressive-stargazing-avalanche.md 的验证清单：
 * <ol>
 *   <li>正常流式：onChunk 被多次调用，onDone 触发，assistant 行在 onDone 时落库</li>
 *   <li>异常路径：onError 触发，assistant 行写入错误摘要</li>
 *   <li>空输入：直接 onDone，不走 Orchestrator，不写 assistant 行</li>
 *   <li>同步入口 handleMessage 拼回完整字符串</li>
 * </ol>
 */
class ChatServiceImplStreamTest {

    private Orchestrator orchestrator;
    private ChatMessageMapper chatMessageMapper;
    private ProfileService profileService;
    private ChatServiceImpl service;

    /** 录制 ChatMessageMapper.insert 的所有调用。 */
    private List<ChatMessage> persisted;

    @BeforeEach
    void setUp() {
        orchestrator = mock(Orchestrator.class);
        chatMessageMapper = mock(ChatMessageMapper.class);
        profileService = mock(ProfileService.class);
        service = new ChatServiceImpl(orchestrator, chatMessageMapper, profileService);

        persisted = new ArrayList<>();
        // 拦截 insert：直接放入内存列表，不依赖 MyBatis-Plus 主键回填
        doAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            persisted.add(msg);
            return 1;
        }).when(chatMessageMapper).insert(any(ChatMessage.class));
    }

    /**
     * 模拟 Orchestrator.dispatchStream：以同步方式把 chunks 推给回调，最后触发 onDone。
     * 用于测试 ChatService 是否正确把 chunk 转给上层 + 在 onDone 时落库。
     */
    private void stubOrchestratorSuccess(String agentType, List<String> chunks) {
        doAnswer(invocation -> {
            AgentMessage input = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onDone = invocation.getArgument(3);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Throwable> onError = invocation.getArgument(4);

            // 模拟 SparkClient 逐 chunk 推送
            for (String c : chunks) {
                onChunk.accept(c);
            }
            onDone.run();
            return null;
        }).when(orchestrator).dispatchStream(eq(agentType), any(AgentMessage.class),
                any(), any(Runnable.class), any());
    }

    /** 模拟 Orchestrator 直接走 onError 路径。 */
    private void stubOrchestratorError(String agentType, Throwable err) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Throwable> onError = invocation.getArgument(4);
            onError.accept(err);
            return null;
        }).when(orchestrator).dispatchStream(eq(agentType), any(AgentMessage.class),
                any(), any(Runnable.class), any());
    }

    @Test
    void stream_normalFlow_chunksPropagatedAndAssistantPersistedOnDone() throws InterruptedException {
        // "讲解线性回归" → 命中"讲解" → agentType=doc
        stubOrchestratorSuccess("doc", List.of("你", "好", "，", "世界"));

        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger();

        service.handleMessageStream(1L, "讲解线性回归",
                chunk -> received.add(chunk),
                latch::countDown,
                err -> errorCount.incrementAndGet());

        assertTrue(latch.await(1, TimeUnit.SECONDS), "onDone 未在 1s 内触发");
        assertEquals(0, errorCount.get());

        // 1. chunk 顺序与原序列一致
        assertEquals(List.of("你", "好", "，", "世界"), received);

        // 2. 一共插入 2 行：user + assistant
        assertEquals(2, persisted.size());
        ChatMessage userMsg = persisted.get(0);
        ChatMessage assistantMsg = persisted.get(1);

        assertEquals("user", userMsg.getRole());
        assertEquals("讲解线性回归", userMsg.getContent());
        assertNull(userMsg.getAgentType(), "user 消息 agentType 应为 null");

        assertEquals("assistant", assistantMsg.getRole());
        assertEquals("你好，世界", assistantMsg.getContent(), "assistant payload 应是完整拼接");
        assertEquals("doc", assistantMsg.getAgentType(), "agentType 应来自意图识别");
    }

    @Test
    void stream_errorFlow_writesErrorAssistantRowAndCallsOnError() throws InterruptedException {
        // "代码示例" → 命中"代码" → agentType=code
        stubOrchestratorError("code", new AIServiceException("讯飞限流 code=10110"));

        List<String> received = new ArrayList<>();
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicInteger doneCount = new AtomicInteger();
        Throwable[] caughtError = new Throwable[1];

        service.handleMessageStream(2L, "代码示例",
                chunk -> received.add(chunk),
                doneCount::incrementAndGet,
                err -> {
                    caughtError[0] = err;
                    errorLatch.countDown();
                });

        assertTrue(errorLatch.await(1, TimeUnit.SECONDS), "onError 未在 1s 内触发");
        assertEquals(0, received.size(), "异常路径不应有 chunk");
        assertEquals(0, doneCount.get(), "异常路径不应调用 onDone");
        assertNotNull(caughtError[0]);
        assertTrue(caughtError[0] instanceof AIServiceException);

        // 落库：user 行 + assistant 错误行
        assertEquals(2, persisted.size());
        ChatMessage assistantMsg = persisted.get(1);
        assertEquals("assistant", assistantMsg.getRole());
        assertTrue(assistantMsg.getContent().startsWith("[错误]"),
                "异常 assistant 消息应以 [错误] 开头，实际：" + assistantMsg.getContent());
        assertTrue(assistantMsg.getContent().contains("讯飞限流"));
        assertEquals("code", assistantMsg.getAgentType());
    }

    @Test
    void stream_emptyInput_doesNotInvokeOrchestratorNorPersistAssistant() throws InterruptedException {
        CountDownLatch doneLatch = new CountDownLatch(1);
        service.handleMessageStream(1L, "  ",
                chunk -> { /* should not be called */ },
                doneLatch::countDown,
                err -> { /* should not be called */ });
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));

        // Orchestrator 完全不应被调用
        verify(orchestrator, never()).dispatchStream(any(), any(), any(), any(), any());
        // 空输入连 user 行都不写
        assertEquals(0, persisted.size());
    }

    @Test
    void stream_nullInput_doesNotInvokeOrchestrator() throws InterruptedException {
        CountDownLatch doneLatch = new CountDownLatch(1);
        service.handleMessageStream(1L, null,
                chunk -> { },
                doneLatch::countDown,
                err -> { });
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
        verify(orchestrator, never()).dispatchStream(any(), any(), any(), any(), any());
    }

    @Test
    void stream_chunkCallbackException_doesNotInterruptSubsequentChunks() throws InterruptedException {
        // 第一次 chunk 抛异常，验证后续 chunk 仍能到达
        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onDone = invocation.getArgument(3);
            onChunk.accept("A");
            try {
                onChunk.accept("B");
            } catch (Exception ignored) {
                /* 模拟上游回调故意抛异常 */
            }
            onChunk.accept("C");
            onDone.run();
            return null;
        }).when(orchestrator).dispatchStream(any(), any(), any(), any(Runnable.class), any());

        service.handleMessageStream(1L, "什么是过拟合",
                chunk -> {
                    if ("B".equals(chunk)) throw new RuntimeException("consumer boom");
                    received.add(chunk);
                },
                latch::countDown,
                err -> { });
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        // A 和 C 都能收到；B 因 consumer 异常被丢弃，但**不影响**后续 C
        assertEquals(List.of("A", "C"), received);
    }

    @Test
    void syncHandleMessage_assemblesFullPayloadFromChunks() throws InterruptedException {
        // 同步入口：内部应该也走流式路径并拼回完整字符串
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onChunk = invocation.getArgument(2);
            Runnable onDone = invocation.getArgument(3);
            for (String c : List.of("线", "性", "回", "归")) {
                onChunk.accept(c);
            }
            onDone.run();
            return null;
        }).when(orchestrator).dispatchStream(eq("doc"), any(AgentMessage.class),
                any(), any(Runnable.class), any());

        String result = service.handleMessage(1L, "讲解线性回归");

        assertEquals("线性回归", result);
        assertEquals(2, persisted.size(), "同步入口也应落库 user + assistant");
        assertEquals("线性回归", persisted.get(1).getContent());
    }

    @Test
    void recognizeIntent_defaultFallbackToTutor() {
        assertEquals("tutor", service.recognizeIntent("随便问问"));
        assertEquals("doc", service.recognizeIntent("讲解下梯度下降"));
        assertEquals("quiz", service.recognizeIntent("给我练习题"));
        assertEquals("code", service.recognizeIntent("代码案例"));
    }
}