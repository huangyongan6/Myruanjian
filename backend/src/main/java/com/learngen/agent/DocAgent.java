package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 课程讲师 DocAgent。
 *
 * <p>对应 CLAUDE.md §4.3：生成 Markdown 课程文档。
 */
@Component
public class DocAgent extends TextOutputAgent {

    public DocAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("DocAgent", "课程讲师", "doc", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.DOC_AGENT_SYSTEM;
    }
}