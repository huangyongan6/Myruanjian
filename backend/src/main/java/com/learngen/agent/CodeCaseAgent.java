package com.learngen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import org.springframework.stereotype.Component;

/**
 * 实战教练 CodeCaseAgent。
 *
 * <p>对应 CLAUDE.md §4.3：生成代码实操案例 JSON。
 */
@Component
public class CodeCaseAgent extends TextOutputAgent {

    public CodeCaseAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("CodeCaseAgent", "实战教练", "code", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.CODE_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{\"description\":\"\",\"dataset\":\"\",\"code\":\"\",\"expected_output\":\"\",\"explanation\":\"\"}";
    }
}