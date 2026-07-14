package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 出题专家 QuizAgent。
 *
 * <p>对应 CLAUDE.md §4.3：生成练习题 JSON。
 */
@Component
public class QuizAgent extends TextOutputAgent {

    public QuizAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("QuizAgent", "出题专家", "quiz", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.QUIZ_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{\"questions\":[]}";
    }
}