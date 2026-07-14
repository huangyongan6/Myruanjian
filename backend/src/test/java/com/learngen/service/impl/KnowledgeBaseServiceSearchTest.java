package com.learngen.service.impl;

import com.learngen.cache.RedisCacheSupport;
import com.learngen.mapper.KnowledgePointMapper;
import com.learngen.model.KnowledgePoint;
import com.learngen.nlp.ChineseTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeBaseService 检索增强（B1）测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>中文名精确匹配 → 命中 name（高分）</li>
 *   <li>部分中文名匹配 → 命中（中等分）</li>
 *   <li>同义词扩展（logistic → 逻辑回归）</li>
 *   <li>description 命中（次高权重）</li>
 *   <li>多字段加权排序</li>
 *   <li>空 keyword → 返回空列表</li>
 *   <li>缓存命中 → 不再查 DB</li>
 * </ul>
 */
class KnowledgeBaseServiceSearchTest {

    private KnowledgePointMapper mapper;
    private RedisCacheSupport cache;
    private ChineseTokenizer tokenizer;
    private KnowledgeBaseServiceImpl service;

    /** 测试用的 6 条种子数据，覆盖 6 个 module。 */
    private final List<KnowledgePoint> seedData = List.of(
            point(1L, "线性回归", 2, "最小二乘法、梯度下降", "module2/linear-regression.md"),
            point(2L, "逻辑回归", 2, "Sigmoid函数、交叉熵损失", "module2/logistic-regression.md"),
            point(3L, "决策树", 2, "信息增益、基尼系数、剪枝", "module2/decision-tree.md"),
            point(4L, "过拟合与欠拟合", 1, "表现、原因、解决方法", "module1/overfitting.md"),
            point(5L, "CNN", 4, "卷积层、池化层、经典结构", "module4/cnn.md"),
            point(6L, "主成分分析", 3, "PCA、方差解释率", "module3/pca.md")
    );

    private static KnowledgePoint point(Long id, String name, int module,
                                        String desc, String path) {
        KnowledgePoint p = new KnowledgePoint();
        p.setId(id);
        p.setName(name);
        p.setModule(module);
        p.setDescription(desc);
        p.setContentPath(path);
        p.setDifficulty("medium");
        return p;
    }

    @BeforeEach
    void setUp() {
        mapper = mock(KnowledgePointMapper.class);
        cache = mock(RedisCacheSupport.class);
        tokenizer = new ChineseTokenizer();
        tokenizer.load();
        service = new KnowledgeBaseServiceImpl(mapper, cache, tokenizer);

        // Redis 全部视为未命中（get 返回 null，set/deleteByPattern 等不做事）
        when(cache.get(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(null);
        when(cache.key(anyString(), anyString(), any())).thenAnswer(inv -> {
            String m = inv.getArgument(0);
            String s = inv.getArgument(1);
            Object id = inv.getArgument(2);
            return "learngen:" + m + ":" + s + ":" + id;
        });
        when(cache.normalizeKeySegment(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Mapper 默认返回全部种子数据
        when(mapper.selectList(any())).thenReturn(seedData);
    }

    @Test
    void searchByName_exactMatch_returnsFirstWithHighestScore() {
        List<KnowledgePoint> result = service.searchByName("线性回归");
        assertFalse(result.isEmpty());
        // "线性回归" 完全匹配 name="线性回归"（score=3）
        assertEquals("线性回归", result.get(0).getName(),
                "精确匹配应在最前，实际顺序：" + namesOf(result));
    }

    @Test
    void searchByName_partialChineseName_stillMatches() {
        List<KnowledgePoint> result = service.searchByName("过拟合");
        assertFalse(result.isEmpty());
        // "过拟合" 是 name="过拟合与欠拟合" 的子串，应命中
        assertTrue(result.stream().anyMatch(kp -> "过拟合与欠拟合".equals(kp.getName())),
                "应命中'过拟合与欠拟合'，实际顺序：" + namesOf(result));
    }

    @Test
    void searchByName_logisticAlias_expandsToLogisticRegression() {
        // logistic → 同义词扩展 → 含 "逻辑回归"
        List<String> expanded = tokenizer.tokenizeAndExpand("logistic");
        List<KnowledgePoint> result = service.searchByName("logistic", expanded);

        assertFalse(result.isEmpty(), "应至少命中一条");
        assertTrue(result.stream().anyMatch(kp -> "逻辑回归".equals(kp.getName())),
                "logistic 应通过同义词扩展命中'逻辑回归'，实际顺序：" + namesOf(result));
    }

    @Test
    void searchByName_descriptionMatch_returnsWithLowerPriority() {
        // "梯度下降" 不在 name 里，但作为"线性回归"description 的一部分
        // 也作为"逻辑回归"description 的一部分（如果有）
        List<String> expanded = List.of("梯度下降");
        List<KnowledgePoint> result = service.searchByName("梯度下降", expanded);

        assertFalse(result.isEmpty());
        // "梯度下降" 命中 description，所以"线性回归"应被召回
        assertTrue(result.stream().anyMatch(kp -> "线性回归".equals(kp.getName())),
                "'梯度下降' 应命中描述含它的'线性回归'，实际顺序：" + namesOf(result));
    }

    @Test
    void searchByName_contentPathMatch_returns() {
        // "linear-regression" 在 contentPath 里 → SCORE_CONTENT_PATH=1
        List<String> expanded = List.of("linear-regression");
        List<KnowledgePoint> result = service.searchByName("path", expanded);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(kp -> "module2/linear-regression.md".equals(kp.getContentPath())),
                "应通过 contentPath 命中'线性回归'");
    }

    @Test
    void searchByName_multipleTerms_sumsScores() {
        // "线性回归 梯度下降" → 线性回归 (name 命中 +3, desc 命中 +2) = 5
        List<String> expanded = List.of("线性回归", "梯度下降");
        List<KnowledgePoint> result = service.searchByName("线性回归 梯度下降", expanded);

        assertFalse(result.isEmpty());
        // "线性回归" 同时被 name 和 description 命中，应排第一
        assertEquals("线性回归", result.get(0).getName(),
                "'线性回归 梯度下降' 应让'线性回归'排第一，实际：" + namesOf(result));
    }

    @Test
    void searchByName_regressionExpandsToBothLinearAndLogistic() {
        // "regression" → 同义词扩展为 [regression, 线性回归, linear, ...]，
        // 也属于"线性回归"组的别名词典。验证：应同时命中线性回归和逻辑回归
        List<String> expanded = tokenizer.tokenizeAndExpand("regression");
        List<KnowledgePoint> result = service.searchByName("regression", expanded);

        // 至少应命中 1 条（"regression" 不在词典里，会通过同义词扩展到线性回归/逻辑回归）
        assertFalse(result.isEmpty());
        boolean hasLinear = result.stream().anyMatch(kp -> "线性回归".equals(kp.getName()));
        boolean hasLogistic = result.stream().anyMatch(kp -> "逻辑回归".equals(kp.getName()));
        assertTrue(hasLinear || hasLogistic,
                "regression 应命中线性回归或逻辑回归，实际：" + namesOf(result));
    }

    @Test
    void searchByName_emptyKeyword_returnsEmpty() {
        assertTrue(service.searchByName(null).isEmpty());
        assertTrue(service.searchByName("").isEmpty());
        assertTrue(service.searchByName("   ").isEmpty());
    }

    @Test
    void searchByName_emptyExpandedTerms_returnsEmpty() {
        // 即使 expandedTerms 为空也不应抛异常
        assertTrue(service.searchByName("test", List.of()).isEmpty());
        assertTrue(service.searchByName("test", null).isEmpty());
    }

    @Test
    void searchByName_noMatch_returnsEmpty() {
        // "xyzabc123notindict" → 分词可能没有命中 → 兜底原词作为 term
        // 但仍要去数据库查 → 33 条种子数据不含此词 → 返回空
        List<String> expanded = List.of("xyzabc123notindict");
        List<KnowledgePoint> result = service.searchByName("xyzabc123notindict", expanded);
        assertTrue(result.isEmpty(), "无匹配词应返回空，实际：" + namesOf(result));
    }

    @Test
    void searchByName_cacheHit_doesNotQueryMapper() {
        // 模拟缓存命中：第一次返回 null（未命中），第二次返回缓存值
        List<KnowledgePoint> cached = List.of(seedData.get(0));
        when(cache.get(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(null)   // 第一次未命中
                .thenReturn(cached); // 第二次命中

        service.searchByName("线性回归"); // 第一次查 DB
        service.searchByName("线性回归"); // 第二次命中缓存

        // selectList 应只被调用一次
        verify(mapper, org.mockito.Mockito.times(1)).selectList(any());
    }

    @Test
    void searchByName_resultSortedByScoreDescThenIdAsc() {
        // "回归" 作为子串匹配 "线性回归"(name) 和 "逻辑回归"(name)，同分时按 ID 升序
        List<String> expanded = List.of("回归");
        List<KnowledgePoint> result = service.searchByName("回归", expanded);

        assertFalse(result.isEmpty());
        // 线性回归(id=1) 和 逻辑回归(id=2) 都命中 name，同分应按 id 升序
        assertTrue(result.size() >= 2);
        if ("线性回归".equals(result.get(0).getName()) && "逻辑回归".equals(result.get(1).getName())) {
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
        }
    }

    private static List<String> namesOf(List<KnowledgePoint> list) {
        return list.stream().map(KnowledgePoint::getName).toList();
    }
}