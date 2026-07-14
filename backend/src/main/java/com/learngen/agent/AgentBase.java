package com.learngen.agent;

import lombok.Getter;

/**
 * Agent 抽象基类。
 *
 * <p>对应 CLAUDE.md §4.1。所有 6 个 Agent 继承此类，统一被 Orchestrator 通过
 * Spring 自动注入收集。
 */
@Getter
public abstract class AgentBase {

    /** Agent 名称（唯一标识，Orchestrator 通过此字段路由） */
    protected final String name;

    /** Agent 角色描述（中文，给前端展示或日志使用） */
    protected final String role;

    /** Agent 类型标识：profile / doc / quiz / reading / code / path */
    protected final String agentType;

    protected AgentBase(String name, String role, String agentType) {
        this.name = name;
        this.role = role;
        this.agentType = agentType;
    }

    /**
     * 处理输入消息并返回结果。
     *
     * <p>子类实现：组合 System Prompt → 调用 SparkClient → 解析输出。
     *
     * @param input 输入消息
     * @return 输出消息（payload 字段承载生成结果）
     */
    public abstract AgentMessage process(AgentMessage input);
}