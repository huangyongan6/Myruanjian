package com.learngen.service.impl;

import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.mapper.ChatMessageMapper;
import com.learngen.model.ChatMessage;
import com.learngen.service.ChatService;
import com.learngen.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 对话服务实现。
 *
 * <p>对应 CLAUDE.md §3.3 Orchestrator 意图识别规则：
 * <ul>
 *   <li>首次对话/介绍自己 → profile</li>
 *   <li>讲解/解释/什么是 → doc</li>
 *   <li>练习/题目/测试 → quiz</li>
 *   <li>推荐/阅读/论文 → reading</li>
 *   <li>代码/实操/案例 → code</li>
 *   <li>学习路径/规划/计划 → path</li>
 *   <li>默认 → doc</li>
 * </ul>
 *
 * <p>同步入口 {@link #handleMessage} 内部委托流式入口 {@link #handleMessageStream}，
 * 保证两条路径行为一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final Orchestrator orchestrator;
    private final ChatMessageMapper chatMessageMapper;
    private final ProfileService profileService;

    /** 关键词 → agentType 的映射表（CLAUDE.md §3.3）。 */
    private static final Map<String, String> INTENT_RULES = Map.ofEntries(
            Map.entry("介绍", "profile"),
            Map.entry("我是", "profile"),
            Map.entry("背景", "profile"),
            Map.entry("讲解", "doc"),
            Map.entry("解释", "doc"),
            Map.entry("什么是", "doc"),
            Map.entry("原理", "doc"),
            Map.entry("练习", "quiz"),
            Map.entry("题目", "quiz"),
            Map.entry("测试", "quiz"),
            Map.entry("做题", "quiz"),
            Map.entry("推荐", "reading"),
            Map.entry("阅读", "reading"),
            Map.entry("论文", "reading"),
            Map.entry("资料", "reading"),
            Map.entry("代码", "code"),
            Map.entry("实操", "code"),
            Map.entry("案例", "code"),
            Map.entry("项目", "code"),
            Map.entry("路径", "path"),
            Map.entry("规划", "path"),
            Map.entry("计划", "path"),
            Map.entry("怎么学", "path")
    );

    @Override
    public String handleMessage(Long studentId, String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "";
        }

        StringBuilder accumulated = new StringBuilder();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        handleMessageStream(studentId, userInput,
                accumulated::append,
                latch::countDown,
                err -> {
                    errorRef.set(err);
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("handleMessage 等待流被中断 studentId={}", studentId);
            Thread.currentThread().interrupt();
        }

        if (errorRef.get() != null) {
            Throwable err = errorRef.get();
            // 同步入口与流式入口一致：异常也以 payload 形式返回（或由 GlobalExceptionHandler 接管）
            // 这里采用保守策略：抛 RuntimeException 让 GlobalExceptionHandler 包装
            if (err instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(err);
        }
        return accumulated.toString();
    }

    @Override
    public void handleMessageStream(Long studentId,
                                    String userInput,
                                    Consumer<String> onChunk,
                                    Runnable onDone,
                                    Consumer<Throwable> onError) {
        // 0. 参数校验：空输入直接结束
        if (userInput == null || userInput.isBlank()) {
            log.debug("handleMessageStream 收到空输入 studentId={}", studentId);
            try {
                onDone.run();
            } catch (Exception e) {
                log.warn("handleMessageStream onDone 回调异常：{}", e.getMessage());
            }
            return;
        }

        // 包装回调：本方法内部所有回调都做容错，调用方拿到的总是单次回调失败不会传染
        Consumer<String> safeChunk = chunk -> {
            if (chunk == null || chunk.isEmpty()) return;
            try {
                onChunk.accept(chunk);
            } catch (Exception e) {
                log.warn("handleMessageStream onChunk 回调异常：{}", e.getMessage());
            }
        };
        Consumer<Throwable> safeError = err -> {
            try {
                onError.accept(err);
            } catch (Exception e) {
                log.warn("handleMessageStream onError 回调异常：{}", e.getMessage());
            }
        };
        Runnable safeDone = () -> {
            try {
                onDone.run();
            } catch (Exception e) {
                log.warn("handleMessageStream onDone 回调异常：{}", e.getMessage());
            }
        };

        // 1. 意图识别
        String agentType = recognizeIntent(userInput);
        log.debug("意图识别：type={} input={}", agentType, userInput);

        // 2. 持久化 user 消息
        try {
            persistMessage(studentId, "user", userInput, null);
        } catch (Exception e) {
            // 持久化失败也要继续推流（不至于整条消息丢失）
            log.warn("持久化 user 消息失败 studentId={} err={}", studentId, e.getMessage());
        }

        // 3. 用 StringBuffer 累积 chunk（线程安全，onDone 时一次性落库）
        StringBuffer payloadBuffer = new StringBuffer();

        // 4. 流式分发
        orchestrator.dispatchStream(agentType,
                AgentMessage.builder()
                        .studentId(studentId)
                        .role("user")
                        .content(userInput)
                        .build(),
                chunk -> {
                    payloadBuffer.append(chunk);
                    safeChunk.accept(chunk);
                },
                () -> {
                    // 流正常结束：写 assistant 行
                    try {
                        persistMessage(studentId, "assistant", payloadBuffer.toString(), agentType);
                    } catch (Exception e) {
                        log.warn("持久化 assistant 消息失败 studentId={} err={}",
                                studentId, e.getMessage());
                    }
                    safeDone.run();
                },
                err -> {
                    // 流异常结束：仍写一条 assistant 行（payload = 错误摘要），保证历史完整
                    String errorPayload = "[错误] " + (err == null ? "未知异常" : err.getMessage());
                    try {
                        persistMessage(studentId, "assistant", errorPayload, agentType);
                    } catch (Exception persistErr) {
                        log.warn("持久化 assistant 错误消息失败 studentId={} err={}",
                                studentId, persistErr.getMessage());
                    }
                    safeError.accept(err);
                });
    }

    /**
     * 简单的关键词匹配意图识别。
     * 第一版：遍历规则表，命中即返回；未命中默认 doc。
     */
    String recognizeIntent(String input) {
        for (Map.Entry<String, String> entry : INTENT_RULES.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "doc";
    }

    private void persistMessage(Long studentId, String role, String content, String agentType) {
        ChatMessage msg = new ChatMessage();
        msg.setStudentId(studentId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setAgentType(agentType);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

    @Override
    public java.util.List<ChatMessage> history(Long studentId, int limit, String before) {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getStudentId, studentId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + limit);
        if (before != null && !before.isBlank()) {
            try {
                java.time.LocalDateTime cursor = java.time.LocalDateTime.parse(before);
                wrapper.lt(ChatMessage::getCreatedAt, cursor);
            } catch (java.time.format.DateTimeParseException ex) {
                // before 格式不合法时按“取最新”处理，避免抛出 500
                log.warn("history: invalid before cursor '{}', fallback to latest", before);
            }
        }
        return chatMessageMapper.selectList(wrapper);
    }
}