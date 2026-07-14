package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 辅导老师 TutorAgent（加分项）。
 *
 * <p>对应 CLAUDE.md §4.3：即时答疑，输出 Markdown 文本（非 JSON）。
 */
@Component
public class TutorAgent extends TextOutputAgent {

    public TutorAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("TutorAgent", "辅导老师", "tutor", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.TUTOR_AGENT_SYSTEM;
    }
}