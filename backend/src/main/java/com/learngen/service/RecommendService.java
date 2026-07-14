package com.learngen.service;

import com.learngen.model.LearningResource;

import java.util.List;

/**
 * 推荐服务接口（CLAUDE.md §4.1 RecommendService）。
 *
 * <p>基于学生画像 + 学习记录，向学生推荐匹配的学习资源。
 */
public interface RecommendService {

    /**
     * 给学生推荐资源列表。
     *
     * <p>推荐策略（MVP 简化版）：
     * <ol>
     *   <li>提取画像中的弱项知识点（weak_points.weak_topics）</li>
     *   <li>查询已生成的资源，优先返回未完成 + 与弱项相关的资源</li>
     *   <li>若不足 N 个，按兴趣方向（interest_area.areas）补充</li>
     * </ol>
     *
     * @param studentId 学生 ID
     * @param limit     最多返回条数（默认 5）
     * @return 推荐资源列表（含 reason 推荐理由）
     */
    List<RecommendedResource> recommend(Long studentId, int limit);

    /** 推荐结果（含资源本体 + 推荐理由）。 */
    record RecommendedResource(LearningResource resource, String reason) {
    }
}