package com.learngen.nlp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChineseTokenizer 单元测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>单 token 拆分（"线性回归" → ["线性回归"]）</li>
 *   <li>多 token 拆分 + 去停用词（"什么是线性回归的代价函数" → 不含停用词）</li>
 *   <li>长词优先（"朴素贝叶斯分类器" 不会被切成"朴素"/"贝叶斯"）</li>
 *   <li>英文大小写不敏感 + 同义词扩展（"logistic" / "Logistic" 都命中"逻辑回归"）</li>
 *   <li>空输入 / 纯停用词 / 边界情况</li>
 * </ul>
 */
class ChineseTokenizerTest {

    private ChineseTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new ChineseTokenizer();
        tokenizer.load();  // 手动调 @PostConstruct 方法
    }

    @Test
    void load_readsDictionaryAndSynonyms() {
        // 词典至少包含一些核心词
        assertTrue(tokenizer.wordCount() > 50, "词典条数应 > 50，实际=" + tokenizer.wordCount());
        // 同义词组至少包含 [逻辑回归, logistic, ...]
        assertTrue(tokenizer.synonymGroupCount() > 0, "应至少有一组同义词");
    }

    @Test
    void tokenize_singleToken_preservesLongWord() {
        List<String> tokens = tokenizer.tokenize("线性回归");
        assertEquals(List.of("线性回归"), tokens);
    }

    @Test
    void tokenize_filtersStopwords() {
        // "什么是线性回归的代价函数"
        // 词典里"线性回归"是词；"代价函数"不在词典但属于专名也不该出现 → 单字被跳过
        // "什么"/"是"/"的"是停用词被过滤
        List<String> tokens = tokenizer.tokenize("什么是线性回归的代价函数");
        assertTrue(tokens.contains("线性回归"), "应包含'线性回归'");
        assertFalse(tokens.contains("什么"), "停用词'什么'应被过滤");
        assertFalse(tokens.contains("是"), "停用词'是'应被过滤");
        assertFalse(tokens.contains("的"), "停用词'的'应被过滤");
    }

    @Test
    void tokenize_longWordPriority() {
        // "朴素贝叶斯" 在词典里优先于 "朴素"/"贝叶斯"
        List<String> tokens = tokenizer.tokenize("朴素贝叶斯");
        assertTrue(tokens.contains("朴素贝叶斯"), "长词'朴素贝叶斯'应被优先切出");
        assertFalse(tokens.contains("朴素"), "不应切出'朴素'");
    }

    @Test
    void tokenize_englishToken_keepsAsOneToken() {
        List<String> tokens = tokenizer.tokenize("SVM 算法");
        assertTrue(tokens.contains("SVM"), "英文 SVM 应作为一个 token");
    }

    @Test
    void tokenize_emptyInput_returnsEmpty() {
        assertTrue(tokenizer.tokenize(null).isEmpty());
        assertTrue(tokenizer.tokenize("").isEmpty());
        assertTrue(tokenizer.tokenize("   ").isEmpty());
    }

    @Test
    void tokenize_allStopwords_returnsEmpty() {
        List<String> tokens = tokenizer.tokenize("什么是");
        // "什么"/"是"都是停用词，理论上会切出后再过滤 → 结果为空
        assertTrue(tokens.isEmpty(), "纯停用词输入应返回空列表，实际=" + tokens);
    }

    @Test
    void tokenize_deduplicatesRepeatedTokens() {
        List<String> tokens = tokenizer.tokenize("线性回归 线性回归 线性回归");
        // 重复出现的 token 应去重
        assertEquals(1, tokens.stream().filter("线性回归"::equals).count(),
                "重复 token 应去重");
    }

    @Test
    void expandSynonyms_logisticExpandsToChineseName() {
        // "logistic" 是英文别名，应扩展到 "逻辑回归" 等同义词
        List<String> expanded = tokenizer.expandSynonyms(List.of("logistic"));
        assertTrue(expanded.contains("logistic"), "原 token 保留");
        assertTrue(expanded.contains("逻辑回归"), "应扩展到主名'逻辑回归'");
        assertTrue(expanded.contains("逻辑斯谛回归"), "应扩展到'逻辑斯谛回归'");
    }

    @Test
    void expandSynonyms_caseInsensitive() {
        // "Logistic" 大写也应命中
        List<String> expanded = tokenizer.expandSynonyms(List.of("Logistic"));
        assertTrue(expanded.contains("逻辑回归"),
                "Logistic 应大小写不敏感地扩展到'逻辑回归'，实际=" + expanded);
    }

    @Test
    void expandSynonyms_unknownToken_returnsOriginal() {
        // 不在同义词组的 token 应原样返回
        List<String> expanded = tokenizer.expandSynonyms(List.of("xyz_unknown"));
        assertEquals(List.of("xyz_unknown"), expanded);
    }

    @Test
    void expandSynonyms_emptyInput_returnsEmpty() {
        assertTrue(tokenizer.expandSynonyms(null).isEmpty());
        assertTrue(tokenizer.expandSynonyms(List.of()).isEmpty());
    }

    @Test
    void tokenizeAndExpand_oneStepConvenience() {
        // "logistic 回归" → 分词：[logistic, 回归] → 同义词扩展 → 含"逻辑回归"
        List<String> result = tokenizer.tokenizeAndExpand("logistic 回归");
        assertNotNull(result);
        assertTrue(result.contains("逻辑回归"),
                "'logistic 回归' 应经分词+同义词扩展到'逻辑回归'，实际=" + result);
    }

    @Test
    void tokenizeAndExpand_chineseAlias() {
        // 主名查自身不会扩展新词，但保留原词
        List<String> result = tokenizer.tokenizeAndExpand("逻辑回归");
        assertTrue(result.contains("逻辑回归"));
    }

    @Test
    void tokenizeAndExpand_svmExpandsToChinese() {
        List<String> result = tokenizer.tokenizeAndExpand("SVM");
        assertTrue(result.contains("支持向量机"),
                "SVM 应扩展到'支持向量机'，实际=" + result);
    }
}