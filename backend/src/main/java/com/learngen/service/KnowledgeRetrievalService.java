package com.learngen.service;

import com.learngen.agent.AgentMessage;

/**
 * 知识库自动检索服务（B 方案）。
 *
 * <p>对应 CLAUDE.md §19 防幻觉 + B 方案"统一召回"。
 * 在 {@link com.learngen.agent.Orchestrator} 调度 Agent 前调用，
 * 按 query（{@code input.getKnowledgePoint()} 或 {@code input.getContent()}）
 * 从知识库拉取相关 Markdown，注入到 {@code input.context.knowledge_preview}。
 *
 * <p>设计原则：
 * <ul>
 *   <li>非破坏性：context 里已有 knowledge_preview（ResourceService / PathService 注入）
 *       时，<b>不覆盖</b>，仅在缺失时填入。</li>
 *   <li>无外部依赖：仅复用 KnowledgeBaseService 的元数据检索 + Markdown 读取。</li>
 *   <li>失败安全：检索过程抛异常时返回原 input，不影响 Agent 调度。</li>
 * </ul>
 */
public interface KnowledgeRetrievalService {

    /**
     * 按 input 携带的 query 自动召回，必要时填充 knowledge_preview。
     *
     * <p>不修改 input 本身（AgentMessage 不可变语义友好），而是返回一个新的 AgentMessage
     * （builder copy），由调用方替换原 input 使用。
     *
     * @param input 原始 AgentMessage
     * @return 携带 knowledge_preview 的新 AgentMessage；若无需召回则原样返回
     */
    AgentMessage enrich(AgentMessage input);
}