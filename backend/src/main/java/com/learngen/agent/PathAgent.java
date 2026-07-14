package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 学习规划师 PathAgent。
 *
 * <p>对应 CLAUDE.md §4.3：基于画像 + 学习记录生成学习路径 JSON。
 */
@Component
public class PathAgent extends TextOutputAgent {

    public PathAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("PathAgent", "学习规划师", "path", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.PATH_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{\"steps\":[]}";
    }
}