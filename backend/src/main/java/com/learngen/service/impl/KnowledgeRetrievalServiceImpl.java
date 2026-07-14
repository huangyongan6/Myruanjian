package com.learngen.service.impl;

import com.learngen.agent.AgentMessage;
import com.learngen.model.KnowledgePoint;
import com.learngen.nlp.ChineseTokenizer;
import com.learngen.service.KnowledgeBaseService;
import com.learngen.service.KnowledgeRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * B 方案实现：在 Orchestrator 调度 Agent 前自动按 query 召回知识库。
 *
 * <p>对应 CLAUDE.md §19 防幻觉 + B 方案"统一召回" + B1 改造（分词 + 同义词）。
 *
 * <p>召回策略：
 * <ol>
 *   <li>query 优先级：{@code input.knowledgePoint} > {@code input.content} 前 80 字</li>
 *   <li>已有 {@code context.knowledge_preview} 且非空 → 跳过（避免覆盖 ResourceService /
 *       PathService 已注入的摘要）</li>
 *   <li>用 {@link ChineseTokenizer} 对 query 做分词 + 同义词扩展，得到 expanded terms</li>
 *   <li>调 {@link KnowledgeBaseService#searchByName(String, List)} 多字段加权打分</li>
 *   <li>取 Top 3 命中，按序读 Markdown，拼接 + 标注来源，总长截断到 {@link #PREVIEW_MAX}</li>
 * </ol>
 *
 * <p>失败安全：检索过程中任何异常被 catch 后 log warn，原样返回 input，不影响 Agent 调度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChineseTokenizer tokenizer;

    /** 注入到 context.knowledge_preview 的总字符上限。 */
    private static final int PREVIEW_MAX = 1800;

    /** 取 Top-N 命中 Markdown。 */
    private static final int TOP_N = 3;

    /** 当没有 knowledgePoint 时，使用 content 的前多少字符作为兜底 query。 */
    private static final int CONTENT_QUERY_CHARS = 80;

    /** 单条 Markdown 截断字符上限（避免单文档过大挤占全部预算）。 */
    private static final int PER_DOC_MAX = 600;

    @Override
    public AgentMessage enrich(AgentMessage input) {
        if (input == null) return null;
        try {
            // 1. 已有 knowledge_preview 则跳过
            Map<String, Object> existingContext = input.getContext();
            if (existingContext != null) {
                Object existing = existingContext.get("knowledge_preview");
                if (existing instanceof String s && !s.isBlank()) {
                    log.debug("知识库自动召回跳过（context 已有 preview，长度={}）", s.length());
                    return input;
                }
            }

            // 2. 解析 query（B1：走分词 + 同义词扩展）
            List<String> expandedTerms = resolveQuery(input);
            if (expandedTerms.isEmpty()) {
                return input;
            }
            String displayQuery = String.join(" ", expandedTerms);

            // 3. 召回（B1：用扩展后的 terms 调多字段加权打分）
            List<KnowledgePoint> hits = knowledgeBaseService.searchByName(
                    displayQuery, expandedTerms);
            if (hits.isEmpty()) {
                log.debug("知识库自动召回无命中 expanded={}", displayQuery);
                return input;
            }
            String preview = buildPreview(hits);
            if (preview.isBlank()) {
                return input;
            }

            // 4. 复制 context，写入 knowledge_preview
            Map<String, Object> newContext = new HashMap<>(
                    existingContext == null ? Map.of() : existingContext);
            newContext.put("knowledge_preview", preview);
            newContext.put("knowledge_query", displayQuery);

            log.info("知识库自动召回 expanded='{}' 命中={}条 preview长度={}",
                    displayQuery, Math.min(hits.size(), TOP_N), preview.length());

            return AgentMessage.builder()
                    .agentName(input.getAgentName())
                    .role(input.getRole())
                    .content(input.getContent())
                    .studentId(input.getStudentId())
                    .knowledgePoint(input.getKnowledgePoint())
                    .difficulty(input.getDifficulty())
                    .context(newContext)
                    .payload(input.getPayload())
                    .build();
        } catch (Exception e) {
            log.warn("知识库自动召回失败，跳过注入：{}", e.getMessage());
            return input;
        }
    }

    /**
     * 解析查询词：优先 knowledgePoint，否则用 content 前 80 字。
     * 走 {@link ChineseTokenizer} 做分词 + 同义词扩展。
     */
    private List<String> resolveQuery(AgentMessage input) {
        String raw;
        if (input.getKnowledgePoint() != null && !input.getKnowledgePoint().isBlank()) {
            raw = input.getKnowledgePoint().trim();
        } else if (input.getContent() == null || input.getContent().isBlank()) {
            return List.of();
        } else {
            String content = input.getContent().trim();
            raw = content.length() > CONTENT_QUERY_CHARS
                    ? content.substring(0, CONTENT_QUERY_CHARS)
                    : content;
        }

        List<String> expanded = tokenizer.tokenizeAndExpand(raw);
        // 兜底：如果分词+同义词扩展后为空（极端情况：全是停用词），
        // 用原词作为单 term，保证至少有一次召回尝试
        if (expanded.isEmpty()) {
            expanded = List.of(raw);
        }
        return expanded;
    }

    /**
     * 按命中顺序拼接 Markdown 摘要，每条标注来源。
     */
    private String buildPreview(List<KnowledgePoint> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("【知识库自动召回】（请优先基于以下事实回答，避免捏造）\n\n");
        for (int i = 0; i < Math.min(hits.size(), TOP_N); i++) {
            KnowledgePoint kp = hits.get(i);
            Optional<String> md = knowledgeBaseService.loadMarkdown(kp);
            if (md.isEmpty()) continue;
            String body = md.get().strip();
            if (body.isEmpty()) continue;

            sb.append("【来源 ").append(i + 1).append("】")
              .append(kp.getName())
              .append("（模块 ").append(kp.getModule() == null ? "?" : kp.getModule())
              .append("，难度 ").append(kp.getDifficulty() == null ? "medium" : kp.getDifficulty())
              .append("）\n");

            // 截断单文档
            if (body.length() > PER_DOC_MAX) {
                body = body.substring(0, PER_DOC_MAX) + "...";
            }
            sb.append(body).append("\n\n");

            if (sb.length() >= PREVIEW_MAX) {
                sb.setLength(PREVIEW_MAX);
                sb.append("...");
                break;
            }
        }
        // 去掉开头标记（若根本没召回任何正文）
        if (sb.toString().equals("【知识库自动召回】（请优先基于以下事实回答，避免捏造）\n\n")) {
            return "";
        }
        return sb.toString();
    }
}