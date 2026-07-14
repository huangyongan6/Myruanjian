package com.learngen.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.ai.PromptTemplates;
import com.learngen.ai.SparkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 学习分析师 ProfileAgent。
 *
 * <p>对应 CLAUDE.md §4.3：从对话中抽取学生 6 维画像。
 *
 * <p>知识库接入（A 方案）：继承 {@link TextOutputAgent} 后，
 * {@code context.knowledge_preview} 会被 {@code buildUserInput} 自动拼到 user prompt。
 * 画像抽取场景一般无需注入知识库正文，但走同一调度链路便于以后扩展。
 */
@Slf4j
@Component
public class ProfileAgent extends TextOutputAgent {

    public ProfileAgent(SparkClient sparkClient, ObjectMapper objectMapper) {
        super("ProfileAgent", "学习分析师", "profile", sparkClient, objectMapper);
    }

    @Override
    protected String systemPrompt() {
        return PromptTemplates.PROFILE_AGENT_SYSTEM;
    }

    @Override
    protected String defaultEmptyPayload() {
        return "{}";
    }

    /**
     * 严格 JSON 标准化：去除 Markdown 代码块包裹并校验 JSON 合法性。
     * 失败时回退原文本（不抛异常），避免上游解析崩溃。
     */
    @Override
    protected String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultEmptyPayload();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("ProfileAgent 输出非合法 JSON，原样返回：长度={}", trimmed.length());
            return trimmed;
        }
    }
}