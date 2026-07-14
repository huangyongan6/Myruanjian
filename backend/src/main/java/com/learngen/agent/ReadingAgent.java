package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 学术推荐官 ReadingAgent。
 *
 * <p>对应 CLAUDE.md §4.3：推荐拓展阅读材料。
 */
@Component
public class ReadingAgent extends TextOutputAgent {

    public ReadingAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("ReadingAgent", "学术推荐官", "reading", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.READING_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{\"items\":[]}";
    }
}