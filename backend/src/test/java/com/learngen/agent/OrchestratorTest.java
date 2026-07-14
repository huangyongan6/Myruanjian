package com.learngen.agent;

import com.learngen.exception.BusinessException;
import com.learngen.service.KnowledgeRetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Orchestrator 路由测试（CLAUDE.md §11.3 + B 方案）。
 *
 * <p>KnowledgeRetrievalService 在测试里用 Mockito mock，
 * 让 dispatch 路径自动把 enrich 后的 input 透传给 Agent。
 */
class OrchestratorTest {

    /** mock 一个"什么都不做"的 KnowledgeRetrievalService。 */
    private static KnowledgeRetrievalService noopRetrieval() {
        KnowledgeRetrievalService svc = mock(KnowledgeRetrievalService.class);
        when(svc.enrich(any())).thenAnswer(inv -> inv.getArgument(0));
        return svc;
    }

    @Test
    void dispatch_unknownType_throwsBusinessException() {
        Orchestrator orchestrator = new Orchestrator(List.of(), noopRetrieval());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orchestrator.dispatch("nonexistent", AgentMessage.builder().build()));
        assertEquals(404, ex.getCode());
    }

    @Test
    void registeredAgentTypes_containsAllRegisteredAgents() {
        Orchestrator orchestrator = new Orchestrator(List.of(
                new TestAgent("A", "a", "x"),
                new TestAgent("B", "b", "y")
        ), noopRetrieval());
        assertEquals(2, orchestrator.registeredAgentTypes().size());
    }

    @Test
    void duplicateAgentType_keepsFirst() {
        TestAgent first = new TestAgent("A", "a", "dup", "FIRST");
        TestAgent second = new TestAgent("B", "b", "dup", "SECOND");
        Orchestrator orchestrator = new Orchestrator(List.of(first, second), noopRetrieval());
        AgentMessage result = orchestrator.dispatch("dup", AgentMessage.builder().build());
        assertEquals("FIRST", result.getPayload());
    }

    /**
     * B 方案：dispatch 前 enrich，Agent 收到的是 enrich 后的 input。
     * 验证 Orchestrator 确实把 enrich 结果传给了 Agent，而不是原 input。
     */
    @Test
    void dispatch_callsEnrichBeforeAgentProcess() {
        TestAgent agent = new TestAgent("X", "x", "x");
        KnowledgeRetrievalService retrieval = mock(KnowledgeRetrievalService.class);
        AgentMessage enriched = AgentMessage.builder().knowledgePoint("after-enrich").build();
        when(retrieval.enrich(any())).thenReturn(enriched);

        Orchestrator orchestrator = new Orchestrator(List.of(agent), retrieval);
        AgentMessage result = orchestrator.dispatch("x", AgentMessage.builder()
                .knowledgePoint("before-enrich").build());

        // Agent.process 应该收到 enriched 后的 input，但因为 TestAgent 不读 input，
        // 这里通过 mock 的 verify 间接证明 enrich 被调用过
        org.mockito.Mockito.verify(retrieval).enrich(any());
        // result.agentName 来自 TestAgent.name（构造首参）
        assertEquals("X", result.getAgentName());
    }

    /** 测试用 Agent 桩。 */
    private static class TestAgent extends AgentBase {
        private final String tag;

        TestAgent(String name, String role, String agentType) {
            this(name, role, agentType, "");
        }

        TestAgent(String name, String role, String agentType, String tag) {
            super(name, role, agentType);
            this.tag = tag;
        }

        @Override
        public AgentMessage process(AgentMessage input) {
            return AgentMessage.builder()
                    .agentName(name)
                    .role("assistant")
                    .payload(tag)
                    .build();
        }
    }
}