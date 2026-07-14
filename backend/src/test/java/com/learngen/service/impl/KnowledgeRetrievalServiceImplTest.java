package com.learngen.service.impl;

import com.learngen.agent.AgentMessage;
import com.learngen.model.KnowledgePoint;
import com.learngen.nlp.ChineseTokenizer;
import com.learngen.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B 方案：KnowledgeRetrievalService 自动召回逻辑测试。
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>context 已有 knowledge_preview → 跳过召回</li>
 *   <li>preview 为空字符串 → 视为缺失，触发召回</li>
 *   <li>无 knowledgePoint 且无 content → 跳过召回</li>
 *   <li>searchByName 无命中 → 跳过召回</li>
 *   <li>正常召回：拼接 Markdown + 标注来源</li>
 *   <li>knowledgeBaseService 抛异常 → 返回原 input（失败安全）</li>
 *   <li>knowledgePoint 优先级高于 content</li>
 * </ol>
 */
class KnowledgeRetrievalServiceImplTest {

    private KnowledgeBaseService knowledgeBaseService;
    private KnowledgeRetrievalServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        tokenizer.load();
        service = new KnowledgeRetrievalServiceImpl(knowledgeBaseService, tokenizer);
    }

    private KnowledgePoint point(Long id, String name, int module, String difficulty) {
        KnowledgePoint p = new KnowledgePoint();
        p.setId(id);
        p.setName(name);
        p.setModule(module);
        p.setDifficulty(difficulty);
        p.setContentPath("module" + module + "/" + name + ".md");
        return p;
    }

    @Test
    void enrich_existingKnowledgePreview_skipsRetrieval() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("knowledge_preview", "已有摘要");
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .content("什么是过拟合")
                .context(ctx)
                .build();

        AgentMessage result = service.enrich(input);

        // 返回原对象，不修改
        assertSame(input, result);
        verify(knowledgeBaseService, never()).searchByName(anyString(), anyList());
    }

    @Test
    void enrich_emptyKnowledgePreview_stillRetrieves() {
        // context 里有 knowledge_preview 但为空字符串 -> 视为缺失，应触发召回
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("knowledge_preview", "");
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("过拟合")
                .context(ctx)
                .build();
        KnowledgePoint kp = point(1L, "过拟合与欠拟合", 1, "medium");
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of(kp));
        when(knowledgeBaseService.loadMarkdown(kp)).thenReturn(Optional.of("正文内容..."));

        AgentMessage result = service.enrich(input);

        assertNotNull(result.getContext().get("knowledge_preview"));
        assertNotEquals("", result.getContext().get("knowledge_preview"));
    }

    @Test
    void enrich_noQuery_returnsSameInput() {
        AgentMessage input = AgentMessage.builder().role("user").build();

        AgentMessage result = service.enrich(input);

        assertSame(input, result);
        verify(knowledgeBaseService, never()).searchByName(anyString(), anyList());
    }

    @Test
    void enrich_emptyQuery_returnsSameInput() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("   ")
                .content("")
                .build();

        AgentMessage result = service.enrich(input);

        assertSame(input, result);
    }

    @Test
    void enrich_noHits_returnsSameInput() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("冷门话题")
                .build();
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of());

        AgentMessage result = service.enrich(input);

        assertSame(input, result);
    }

    @Test
    void enrich_knowledgePointPriority_overContent() {
        // knowledgePoint 存在时优先使用，content 不参与
        String longContent = "a".repeat(500);
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("决策树")
                .content(longContent)
                .build();
        KnowledgePoint kp = point(2L, "决策树", 2, "medium");
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of(kp));
        when(knowledgeBaseService.loadMarkdown(kp)).thenReturn(Optional.of("信息增益..."));

        service.enrich(input);

        // 验证被调用（任意 keyword 与 expanded terms 都匹配）
        verify(knowledgeBaseService).searchByName(anyString(), anyList());
    }

    @Test
    void enrich_normalRetrieval_buildsPreviewWithSourceLabels() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("过拟合")
                .build();
        KnowledgePoint kp = point(1L, "过拟合与欠拟合", 1, "medium");
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of(kp));
        when(knowledgeBaseService.loadMarkdown(kp)).thenReturn(Optional.of("## 表现\n训练误差低，测试误差高"));

        AgentMessage result = service.enrich(input);

        String preview = (String) result.getContext().get("knowledge_preview");
        assertNotNull(preview);
        assertTrue(preview.contains("知识库自动召回"));
        assertTrue(preview.contains("过拟合与欠拟合"));
        assertTrue(preview.contains("模块 1"));
        assertTrue(preview.contains("medium"));
        assertTrue(preview.contains("训练误差低"));
        // B1 改造后 knowledge_query 是扩展后的 term 字符串，不再是原 keyword
        assertNotNull(result.getContext().get("knowledge_query"));
    }

    @Test
    void enrich_topNLimitsHits() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("算法")
                .build();
        // 给 5 条命中，但应只取前 3 条
        List<KnowledgePoint> many = List.of(
                point(1L, "A", 1, "easy"),
                point(2L, "B", 2, "easy"),
                point(3L, "C", 3, "easy"),
                point(4L, "D", 4, "easy"),
                point(5L, "E", 5, "easy")
        );
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(many);
        when(knowledgeBaseService.loadMarkdown(any())).thenReturn(Optional.of("正文"));

        AgentMessage result = service.enrich(input);

        String preview = (String) result.getContext().get("knowledge_preview");
        // 标注【来源 1】【来源 2】【来源 3】共 3 个
        assertTrue(preview.contains("【来源 1】"));
        assertTrue(preview.contains("【来源 2】"));
        assertTrue(preview.contains("【来源 3】"));
        assertTrue(!preview.contains("【来源 4】"), "不应出现第 4 条来源");
    }

    @Test
    void enrich_loadMarkdownEmpty_skipsThatHit() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("X")
                .build();
        KnowledgePoint kp1 = point(1L, "X1", 1, "easy");
        KnowledgePoint kp2 = point(2L, "X2", 2, "easy");
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of(kp1, kp2));
        // 第一条 loadMarkdown 返回空，第二条返回正文
        when(knowledgeBaseService.loadMarkdown(kp1)).thenReturn(Optional.of(""));
        when(knowledgeBaseService.loadMarkdown(kp2)).thenReturn(Optional.of("实际正文"));

        AgentMessage result = service.enrich(input);

        String preview = (String) result.getContext().get("knowledge_preview");
        assertTrue(preview.contains("实际正文"));
        assertTrue(!preview.contains("X1"), "空内容文档不应进入 preview");
    }

    @Test
    void enrich_serviceThrows_returnsOriginalInput() {
        AgentMessage input = AgentMessage.builder()
                .role("user")
                .knowledgePoint("Y")
                .build();
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenThrow(new RuntimeException("DB 崩了"));

        AgentMessage result = service.enrich(input);

        // 失败安全：返回原对象，不抛异常
        assertSame(input, result);
    }

    @Test
    void enrich_enrichesContextWithoutLosingOtherFields() {
        // 验证 enrich 后 input 的其他字段（studentId / difficulty 等）不丢
        AgentMessage input = AgentMessage.builder()
                .agentName("caller")
                .role("user")
                .studentId(42L)
                .knowledgePoint("PCA")
                .difficulty("hard")
                .content("主成分分析是什么")
                .build();
        KnowledgePoint kp = point(3L, "PCA主成分分析", 3, "medium");
        when(knowledgeBaseService.searchByName(anyString(), anyList())).thenReturn(List.of(kp));
        when(knowledgeBaseService.loadMarkdown(kp)).thenReturn(Optional.of("降维..."));

        AgentMessage result = service.enrich(input);

        assertEquals(42L, result.getStudentId());
        assertEquals("hard", result.getDifficulty());
        assertEquals("主成分分析是什么", result.getContent());
        assertEquals("caller", result.getAgentName());
        assertNotNull(result.getContext().get("knowledge_preview"));
    }
}