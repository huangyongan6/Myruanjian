package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.KnowledgePointMapper;
import com.learngen.model.KnowledgePoint;
import com.learngen.nlp.ChineseTokenizer;
import com.learngen.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 知识库服务实现。
 *
 * <p>B1 改造：检索从「单字段 LIKE」升级到「分词 + 同义词 + 多字段加权打分」。
 *
 * <p>对应 CLAUDE.md §4.1：构造器注入，Mapper 操作走 LambdaQueryWrapper，无 SQL 拼接。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /** Markdown 根目录相对 classpath 路径（与 doc/05-机器学习知识库规划.md 对齐）。 */
    private static final String KNOWLEDGE_ROOT = "knowledge/";

    /** 字段加权：name 命中权重最高，description 次之，contentPath 兜底。 */
    private static final int SCORE_NAME = 3;
    private static final int SCORE_DESCRIPTION = 2;
    private static final int SCORE_CONTENT_PATH = 1;

    private final KnowledgePointMapper knowledgePointMapper;
    private final RedisCacheSupport cache;
    private final ChineseTokenizer tokenizer;

    @Override
    public List<KnowledgePoint> listByModule(Integer module) {
        List<KnowledgePoint> cached = cache.get(
                cache.key("knowledge", "byModule", module),
                new TypeReference<List<KnowledgePoint>>() {});
        if (cached != null) return cached;

        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePoint::getModule, module)
                .orderByAsc(KnowledgePoint::getId);
        List<KnowledgePoint> list = knowledgePointMapper.selectList(wrapper);
        cache.set(cache.key("knowledge", "byModule", module),
                list, RedisCacheSupport.TTL_KNOWLEDGE_LIST);
        return list;
    }

    @Override
    public Optional<KnowledgePoint> findById(Long id) {
        KnowledgePoint cached = cache.get(
                cache.key("knowledge", "byId", id), KnowledgePoint.class);
        if (cached != null) return Optional.of(cached);

        KnowledgePoint point = knowledgePointMapper.selectById(id);
        if (point != null) {
            cache.set(cache.key("knowledge", "byId", id),
                    point, RedisCacheSupport.TTL_KNOWLEDGE_BY_ID);
        }
        return Optional.ofNullable(point);
    }

    /**
     * 单 keyword 兼容入口：内部委托分词+同义词扩展，行为与新接口一致。
     * 保留此重载为了兼容老调用方（CLAUDE.md §3.5 不破坏现有行为）。
     */
    @Override
    public List<KnowledgePoint> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        List<String> expanded = tokenizer.tokenizeAndExpand(keyword);
        // 兜底：如果分词后为空（如纯停用词），用原 keyword 当作单个 term
        if (expanded.isEmpty()) {
            expanded = List.of(keyword.trim());
        }
        return searchByName(keyword, expanded);
    }

    /**
     * 增强检索（B1）：分词 + 同义词 + 多字段加权打分。
     *
     * <p>实现策略：
     * <ol>
     *   <li>按 expandedTerms 拼接成 cache key（排序去重，保证大小写无关）</li>
     *   <li>命中缓存直接返回</li>
     *   <li>未命中：全表 selectList（33 条记录，可接受），在 Java 端对每条记录按 term 累加分数</li>
     *   <li>分数 &gt; 0 的按 (score desc, id asc) 排序，写缓存后返回</li>
     * </ol>
     */
    @Override
    public List<KnowledgePoint> searchByName(String keyword, List<String> expandedTerms) {
        if (keyword == null || keyword.isBlank()) return List.of();
        if (expandedTerms == null || expandedTerms.isEmpty()) return List.of();

        // 1. 拼缓存 key：原 keyword + 扩展后 terms 排序去重（避免大小写/顺序导致的缓存分裂）
        String sortedTerms = expandedTerms.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .reduce((a, b) -> a + "+" + b)
                .orElse("");
        if (sortedTerms.isEmpty()) return List.of();

        String seg = cache.normalizeKeySegment(keyword + "|" + sortedTerms);
        String cacheKey = cache.key("knowledge", "byName", seg);

        List<KnowledgePoint> cached = cache.get(cacheKey,
                new TypeReference<List<KnowledgePoint>>() {});
        if (cached != null) return cached;

        // 2. 全表扫描（数据集小，< 100 条，无性能问题）
        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(KnowledgePoint::getModule)
                .orderByAsc(KnowledgePoint::getId);
        List<KnowledgePoint> all = knowledgePointMapper.selectList(wrapper);

        // 3. Java 端打分
        List<ScoredPoint> scored = new ArrayList<>();
        for (KnowledgePoint kp : all) {
            int score = scoreOf(kp, expandedTerms);
            if (score > 0) {
                scored.add(new ScoredPoint(kp, score));
            }
        }

        // 4. 排序：分数降序 → ID 升序
        scored.sort(Comparator
                .comparingInt(ScoredPoint::score).reversed()
                .thenComparingLong(sp -> sp.point.getId()));

        List<KnowledgePoint> result = scored.stream().map(ScoredPoint::point).toList();

        // 5. 写缓存
        cache.set(cacheKey, result, RedisCacheSupport.TTL_KNOWLEDGE_SEARCH);
        log.debug("知识库检索 keyword='{}' expanded={}条 命中={}条",
                keyword, expandedTerms.size(), result.size());
        return result;
    }

    /**
     * 对单个 KnowledgePoint 计算总分：遍历每个 term，对 name/description/contentPath 三字段累加。
     * 字段匹配用 {@code String.contains}（小写比较），简单高效。
     */
    private int scoreOf(KnowledgePoint kp, List<String> terms) {
        String name = lowerOrEmpty(kp.getName());
        String desc = lowerOrEmpty(kp.getDescription());
        String path = lowerOrEmpty(kp.getContentPath());
        int score = 0;
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            String t = term.toLowerCase(Locale.ROOT);
            if (name.contains(t)) score += SCORE_NAME;
            if (desc.contains(t)) score += SCORE_DESCRIPTION;
            if (path.contains(t)) score += SCORE_CONTENT_PATH;
        }
        return score;
    }

    private static String lowerOrEmpty(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<String> loadMarkdown(KnowledgePoint point) {
        if (point == null || point.getContentPath() == null || point.getContentPath().isBlank()) {
            return Optional.empty();
        }
        String path = point.getContentPath();
        // 防御：防止路径穿越（CLAUDE.md §14.4：禁止异常吞掉）
        if (path.contains("..")) {
            log.warn("拒绝读取穿越路径：{}", path);
            return Optional.empty();
        }
        String resourcePath = KNOWLEDGE_ROOT + path;
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.debug("Markdown 不存在：{}", resourcePath);
                return Optional.empty();
            }
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return Optional.of(content);
        } catch (IOException e) {
            log.warn("读取 Markdown 失败：{} cause={}", resourcePath, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> loadMarkdownByName(String keyword) {
        return searchByName(keyword).stream()
                .findFirst()
                .flatMap(this::loadMarkdown);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePoint create(KnowledgePoint point) {
        validate(point);
        point.setId(null);
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        knowledgePointMapper.insert(point);
        // 写后失效：byId:{id} + 全部 module 列表 + 全部 name 搜索结果
        cache.delete(cache.key("knowledge", "byId", point.getId()));
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byModule:*");
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byName:*");
        log.info("知识点创建 id={} name={}", point.getId(), point.getName());
        return point;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePoint update(Long id, KnowledgePoint point) {
        KnowledgePoint existing = knowledgePointMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "知识点不存在：id=" + id);
        }
        if (point.getName() != null) existing.setName(point.getName());
        if (point.getDescription() != null) existing.setDescription(point.getDescription());
        if (point.getContentPath() != null) existing.setContentPath(point.getContentPath());
        if (point.getDifficulty() != null) existing.setDifficulty(point.getDifficulty());
        if (point.getModule() != null) existing.setModule(point.getModule());
        existing.setUpdatedAt(LocalDateTime.now());
        knowledgePointMapper.updateById(existing);
        // 写后失效：module 与 name 都可能已变，全量失效列表/搜索缓存
        cache.delete(cache.key("knowledge", "byId", id));
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byModule:*");
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byName:*");
        log.info("知识点更新 id={}", id);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        int rows = knowledgePointMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(404, "知识点不存在：id=" + id);
        }
        cache.delete(cache.key("knowledge", "byId", id));
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byModule:*");
        cache.deleteByPattern(RedisCacheSupport.NAMESPACE + ":knowledge:byName:*");
        log.info("知识点删除 id={}", id);
    }

    private void validate(KnowledgePoint point) {
        if (point == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        if (point.getModule() == null || point.getModule() < 1 || point.getModule() > 6) {
            throw new BusinessException(400, "module 必须在 1~6 之间");
        }
        if (point.getName() == null || point.getName().isBlank()) {
            throw new BusinessException(400, "name 不能为空");
        }
        if (point.getContentPath() != null && point.getContentPath().contains("..")) {
            throw new BusinessException(400, "contentPath 不允许包含 ..");
        }
    }

    /** 内部类：打分结果。 */
    private record ScoredPoint(KnowledgePoint point, int score) {
    }
}