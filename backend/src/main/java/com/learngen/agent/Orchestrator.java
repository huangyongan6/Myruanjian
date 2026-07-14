package com.learngen.agent;

import com.learngen.exception.BusinessException;
import com.learngen.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 多智能体编排器。
 *
 * <p>对应 CLAUDE.md §11.3：通过 Spring 自动注入所有 {@link AgentBase} 实现类，
 * 按 {@code agentType} 路由。新增 Agent 只需加 {@code @Component}，无需修改本类。
 *
 * <p>当前轮次：按类型路由 + 兜底异常。
 *
 * <p>B 方案：构造器注入 {@link KnowledgeRetrievalService}。
 * 在每次 {@link #dispatch} 调用 Agent 前，自动按 query（knowledgePoint / content）
 * 从知识库召回相关 Markdown 注入 {@code input.context.knowledge_preview}。
 * 已有 preview 的 context 不被覆盖（保护 ResourceService / PathService 的显式注入）。
 */
@Slf4j
@Component
public class Orchestrator {

    /** key = agentType（profile / doc / mindmap / quiz / reading / code / path / tutor） */
    private final Map<String, AgentBase> agentByType;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public Orchestrator(List<AgentBase> agents, KnowledgeRetrievalService knowledgeRetrievalService) {
        this.agentByType = agents.stream()
                .collect(Collectors.toMap(AgentBase::getAgentType, a -> a, (a, b) -> {
                    log.warn("检测到重复的 agentType={}，保留第一个 {}", a.getAgentType(), a.getName());
                    return a;
                }));
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        log.info("Orchestrator 注册 Agent：{}（B 方案知识库自动召回已启用）", agentByType.keySet());
    }

    /**
     * 按 agentType 分发到对应 Agent。
     *
     * <p>B 方案：在分发前调用 {@link KnowledgeRetrievalService#enrich}，
     * 把知识库召回结果注入 context.knowledge_preview。
     *
     * @param agentType 类型标识，对应 CLAUDE.md §4.3 8 种 Agent
     * @param input     输入消息
     * @return Agent 输出
     * @throws BusinessException 404 当 agentType 未注册时
     */
    public AgentMessage dispatch(String agentType, AgentMessage input) {
        AgentBase agent = agentByType.get(agentType);
        if (agent == null) {
            throw new BusinessException(404, "未注册的 Agent 类型：" + agentType);
        }
        // B 方案：自动召回，失败时 enrich 内部已 catch 返回原 input
        AgentMessage enriched = knowledgeRetrievalService.enrich(input);
        log.debug("Orchestrator 分发：type={} agent={}", agentType, agent.getName());
        return agent.process(enriched);
    }

    /**
     * 流式分发：把 Agent 的输出逐 chunk 通过回调推送给调用方。
     *
     * <p>Agent 实现 {@link StreamableAgent} 时走流式分支（推荐路径，对应
     * WebSocket 打字机效果）；未实现时走兜底同步分支（一次推送完整 payload）。
     *
     * <p>回调契约（与 {@link StreamableAgent} 一致）：onDone 与 onError 互斥，
     * 任一分支内对回调的容错由 Agent 实现负责，本方法不做二次 try-catch 包裹，
     * 避免吞掉真正的业务异常。
     *
     * @param agentType 类型标识
     * @param input     输入消息
     * @param onChunk   每片内容回调（StreamableAgent 可能调用 0..N 次；
     *                  兜底分支调用 1 次传完整 payload）
     * @param onDone    结束回调
     * @param onError   异常回调
     */
    public void dispatchStream(String agentType,
                               AgentMessage input,
                               Consumer<String> onChunk,
                               Runnable onDone,
                               Consumer<Throwable> onError) {
        AgentBase agent = agentByType.get(agentType);
        if (agent == null) {
            try {
                onError.accept(new BusinessException(404, "未注册的 Agent 类型：" + agentType));
            } catch (Exception e) {
                log.warn("dispatchStream onError 回调异常：{}", e.getMessage());
            }
            return;
        }
        // B 方案：自动召回，失败时 enrich 内部已 catch 返回原 input
        AgentMessage enriched = knowledgeRetrievalService.enrich(input);
        log.debug("Orchestrator 流式分发：type={} agent={} streamable={}",
                agentType, agent.getName(), agent instanceof StreamableAgent);

        if (agent instanceof StreamableAgent sa) {
            sa.processStream(enriched, onChunk, onDone, onError);
            return;
        }

        // 兜底分支：老 Agent 未实现流式接口 → 同步执行后一次推完整 payload
        try {
            AgentMessage out = agent.process(enriched);
            if (out != null && out.getPayload() != null && !out.getPayload().isEmpty()) {
                onChunk.accept(out.getPayload());
            }
            try {
                onDone.run();
            } catch (Exception e) {
                log.warn("dispatchStream 兜底 onDone 回调异常：{}", e.getMessage());
            }
        } catch (Exception e) {
            try {
                onError.accept(e);
            } catch (Exception inner) {
                log.warn("dispatchStream 兜底 onError 回调异常：{}", inner.getMessage());
            }
        }
    }

    /** 当前已注册的 Agent 类型列表（用于调试或健康检查）。 */
    public java.util.Set<String> registeredAgentTypes() {
        return agentByType.keySet();
    }
}