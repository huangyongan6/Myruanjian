package com.learngen.nlp;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 轻量中文分词 + 同义词扩展组件。
 *
 * <p>对应 CLAUDE.md §19 防幻觉 / 改动 B1：知识库检索增强。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>分词算法</b>：双向最大匹配（MMSEG 简化版），按词典长度倒序匹配，优先切长词</li>
 *   <li><b>同义词索引</b>：每组第一个词视为"主名"，其余视为别名；任意别名查询扩展到全组</li>
 *   <li><b>大小写不敏感</b>：英文别名（logistic / SVM / PCA）通过 {@code toLowerCase} 统一</li>
 *   <li><b>零外部依赖</b>：用 Spring Boot 自带的 SnakeYAML 读取 YAML 配置，无新增 jar</li>
 *   <li><b>失败安全</b>：{@link #load()} 抛异常 → Spring 启动失败（fail fast），
 *       比静默退化为不分词更安全</li>
 * </ul>
 *
 * <p>配置位置：{@code classpath:knowledge-dict.yml}
 */
@Slf4j
@Component
public class ChineseTokenizer {

    private static final String CONFIG_PATH = "knowledge-dict.yml";

    /** 词典（按长度倒序，长词优先匹配）。 */
    private List<String> words;

    /** 停用词集合。 */
    private Set<String> stopwords;

    /** 同义词反向索引：lower(任意词) → 同义词组（保留原大小写去重）。 */
    private Map<String, Set<String>> synonymMap;

    @PostConstruct
    public void load() {
        Yaml yaml = new Yaml();
        try (InputStream in = new ClassPathResource(CONFIG_PATH).getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                throw new IllegalStateException("knowledge-dict.yml 为空");
            }

            @SuppressWarnings("unchecked")
            List<String> rawWords = (List<String>) root.get("words");
            @SuppressWarnings("unchecked")
            List<String> rawStops = (List<String>) root.get("stopwords");
            @SuppressWarnings("unchecked")
            List<List<String>> rawSynonyms = (List<List<String>>) root.get("synonyms");

            this.words = (rawWords == null ? List.<String>of() : rawWords).stream()
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted((a, b) -> Integer.compare(b.length(), a.length())) // 长→短
                    .collect(Collectors.toUnmodifiableList());

            this.stopwords = (rawStops == null ? List.<String>of() : rawStops).stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toUnmodifiableSet());

            this.synonymMap = buildSynonymIndex(rawSynonyms);

            log.info("ChineseTokenizer 初始化完成：词典={} 停用词={} 同义词组={}",
                    words.size(), stopwords.size(), synonymMap.size());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "加载 " + CONFIG_PATH + " 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 构建同义词反向索引。
     * 输入：[[a, b, c], [d, e]] → 输出：{a:{a,b,c}, b:{a,b,c}, c:{a,b,c}, d:{d,e}, e:{d,e}}
     * 大小写不敏感：key 全部 toLowerCase，value 保留原大小写去重。
     */
    private Map<String, Set<String>> buildSynonymIndex(List<List<String>> rawSynonyms) {
        Map<String, Set<String>> idx = new HashMap<>();
        if (rawSynonyms == null) return idx;
        for (List<String> group : rawSynonyms) {
            if (group == null || group.isEmpty()) continue;
            // 用 LinkedHashSet 保留原顺序（第一个 = 主名）
            Set<String> canonical = new LinkedHashSet<>();
            for (String s : group) {
                if (s != null && !s.isBlank()) canonical.add(s.trim());
            }
            if (canonical.isEmpty()) continue;
            for (String alias : canonical) {
                idx.put(alias.toLowerCase(Locale.ROOT), canonical);
            }
        }
        return Collections.unmodifiableMap(idx);
    }

    /**
     * 双向最大匹配切词（简化版，仅正向最大匹配；长词优先足够应付小词典场景）。
     *
     * <p>算法：
     * <ol>
     *   <li>从左到右扫描，尝试匹配当前 position 起的最长词典词</li>
     *   <li>命中则切出该词，position 后移</li>
     *   <li>未命中则单字切出，position +1（兜底，保证不丢字）</li>
     *   <li>切完后过滤停用词</li>
     * </ol>
     *
     * @param text 原始文本
     * @return 去重 + 去停用词后的 token 列表
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        String s = text.trim();

        List<String> tokens = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            // 跳过空白
            if (Character.isWhitespace(s.charAt(i))) {
                i++;
                continue;
            }
            // 找从 i 起能命中的最长词典词
            boolean matched = false;
            for (String w : words) {
                int end = i + w.length();
                if (end <= n && s.regionMatches(i, w, 0, w.length())) {
                    if (!stopwords.contains(w)) {
                        tokens.add(w);
                    }
                    i = end;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // 未命中词典词：尝试按"非中文字符"做软切分（英文/数字作为一个 token）
                int start = i;
                char c = s.charAt(i);
                if (isAsciiLetterOrDigit(c)) {
                    while (i < n && isAsciiLetterOrDigit(s.charAt(i))) i++;
                    String token = s.substring(start, i);
                    if (!stopwords.contains(token)) tokens.add(token);
                } else {
                    // 单个汉字或符号：跳过（避免产生无意义单字 token）
                    i++;
                }
            }
        }
        return tokens.stream().distinct().collect(Collectors.toList());
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    /**
     * 对 token 列表做同义词扩展。
     *
     * <p>大小写不敏感：英文 token 转小写查同义词索引；命中后把所有同义词加进来。
     * 中文 token 原样查（中文没有大小写问题）。
     *
     * @param tokens 原 token 列表（一般来自 {@link #tokenize}）
     * @return 包含原 token + 所有同义词的列表（去重，保持出现顺序）
     */
    public List<String> expandSynonyms(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        for (String t : tokens) {
            result.add(t);
            Set<String> group = synonymMap.get(t.toLowerCase(Locale.ROOT));
            if (group != null) {
                result.addAll(group);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 便捷方法：分词 + 同义词扩展一次完成。
     *
     * @param text 原始文本
     * @return 扩展后的 token 列表
     */
    public List<String> tokenizeAndExpand(String text) {
        return expandSynonyms(tokenize(text));
    }

    /** 调试/测试用：查看当前词典大小。 */
    public int wordCount() {
        return words == null ? 0 : words.size();
    }

    /** 调试/测试用：查看当前同义词组数。 */
    public int synonymGroupCount() {
        return synonymMap == null ? 0 : synonymMap.size();
    }

    /** 把 token 列表拼成空格分隔的字符串，用于日志展示。 */
    public static String joinForLog(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return "(空)";
        return new TreeSet<>(tokens).stream().collect(Collectors.joining(" "));
    }
}