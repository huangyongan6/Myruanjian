package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 知识架构师 MindMapAgent。
 *
 * <p>对应 CLAUDE.md §4.3：生成 JSON 树结构，前端 markmap 渲染。
 */
@Component
public class MindMapAgent extends TextOutputAgent {

    public MindMapAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("MindMapAgent", "知识架构师", "mindmap", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.MINDMAP_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{\"tree\":{\"name\":\"empty\",\"children\":[]}}";
    }
}