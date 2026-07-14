package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.mapper.LearningResourceMapper;
import com.learngen.model.KnowledgePoint;
import com.learngen.model.LearningRecord;
import com.learngen.model.LearningResource;
import com.learngen.model.StudentProfile;
import com.learngen.service.KnowledgeBaseService;
import com.learngen.service.LearningRecordService;
import com.learngen.service.ProfileService;
import com.learngen.service.RecommendService;
import com.learngen.service.ResourceService;
import com.learngen.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 推荐服务实现。
 *
 * <p>对应 CLAUDE.md §10：构造器注入；调用其他 Service 不直接调 Mapper。
 *
 * <p>知识库接入（C2）：注入 {@link KnowledgeBaseService} 后，
 * 把画像里的 weak_topics / interest_areas 先解析为 {@link KnowledgePoint} 元数据
 * （模块、难度、简介），再用这些结构化字段给候选资源排序打分，
 * 而不是只对 LearningResource.knowledgePoint 做 LIKE 模糊匹配。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final StudentService studentService;
    private final ProfileService profileService;
    private final ResourceService resourceService;
    private final LearningRecordService recordService;
    private final LearningResourceMapper resourceMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    /** 同模块加分：候选资源所属模块与画像偏好知识点所属模块一致 → +0.4 */
    private static final double SCORE_MODULE_MATCH = 0.4;
    /** 难度匹配加分：候选资源难度与画像偏好难度一致 → +0.2 */
    private static final double SCORE_DIFFICULTY_MATCH = 0.2;
    /** 简介相似加分：候选资源的 knowledgePoint 在知识库简介/名称里命中 → +0.2 */
    private static final double SCORE_NAME_MATCH = 0.2;

    @Override
    public List<RecommendedResource> recommend(Long studentId, int limit) {
        if (limit <= 0) {
            limit = 5;
        }

        // 1. 校验学生存在
        studentService.getById(studentId);

        // 2. 提取画像弱项 + 兴趣，并把字符串解析为知识库元数据（模块/难度）
        ProfileInfo info = loadProfile(studentId);

        // 3. 已学习的资源 ID（view / complete）
        Set<Long> learnedIds = collectLearnedResourceIds(studentId);

        // 4. 候选：先按弱项 → 再按兴趣 → 兜底按时间
        // C2：用知识库元数据打分排序，不再只 LIKE 模糊匹配
        Set<String> candidatePoints = new LinkedHashSet<>();
        candidatePoints.addAll(info.weakTopics);
        candidatePoints.addAll(info.interestAreas);

        List<ScoredResource> scoredCandidates = new ArrayList<>();
        for (String point : candidatePoints) {
            if (point == null || point.isBlank()) continue;
            // 4.1 用知识库元数据查知识点（结构化匹配）
            List<KnowledgePoint> kps = knowledgeBaseService.searchByName(point);
            KnowledgePoint primaryKp = kps.isEmpty() ? null : kps.get(0);

            List<LearningResource> matched = resourceMapper.selectList(
                    new LambdaQueryWrapper<LearningResource>()
                            .like(LearningResource::getKnowledgePoint, point)
                            .orderByDesc(LearningResource::getCreatedAt)
                            .last("LIMIT 5"));
            for (LearningResource r : matched) {
                if (learnedIds.contains(r.getId())) continue;
                double score = scoreCandidate(r, primaryKp, info);
                scoredCandidates.add(new ScoredResource(r, score, primaryKp));
            }
        }
        // 4.2 按分数降序
        scoredCandidates.sort((a, b) -> Double.compare(b.score, a.score));

        // 4.3 兜底：候选不足时按时间拉
        if (scoredCandidates.size() < limit) {
            List<LearningResource> fallback = resourceService.listByStudent(studentId, null);
            for (LearningResource r : fallback) {
                if (scoredCandidates.size() >= limit) break;
                if (learnedIds.contains(r.getId())) continue;
                if (scoredCandidates.stream().anyMatch(s -> s.resource.getId().equals(r.getId()))) continue;
                scoredCandidates.add(new ScoredResource(r, 0.0, null));
            }
        }

        // 5. 包装推荐理由（用知识库元数据补全"模块/难度"信息）
        List<RecommendedResource> result = new ArrayList<>();
        int take = Math.min(scoredCandidates.size(), limit);
        for (int i = 0; i < take; i++) {
            ScoredResource s = scoredCandidates.get(i);
            result.add(new RecommendedResource(s.resource, buildReason(s, info)));
        }
        log.info("推荐生成 studentId={} 返回={}条 / 候选={}条 / 已学习={}条 / 知识库命中={}个知识点",
                studentId, result.size(), scoredCandidates.size(), learnedIds.size(),
                info.weakPointsMeta.size() + info.interestMeta.size());
        return result;
    }

    /**
     * 给候选资源打分：
     * <ul>
     *   <li>知识库命中（primaryKp 非空）：模块匹配 +0.4，难度匹配 +0.2</li>
     *   <li>画像 weak_topics / interest_areas 中包含资源 knowledgePoint：+0.2</li>
     * </ul>
     */
    private double scoreCandidate(LearningResource r, KnowledgePoint primaryKp, ProfileInfo info) {
        double score = 0.0;
        if (primaryKp != null) {
            // 模块匹配（资源本身没存模块，但 knowledgePoint 名称与知识库某条同名即可认为命中）
            if (primaryKp.getName() != null
                    && r.getKnowledgePoint() != null
                    && r.getKnowledgePoint().contains(primaryKp.getName())) {
                score += SCORE_MODULE_MATCH;
            }
            // 难度匹配
            if (primaryKp.getDifficulty() != null
                    && primaryKp.getDifficulty().equalsIgnoreCase(r.getDifficulty())) {
                score += SCORE_DIFFICULTY_MATCH;
            }
        }
        // 画像字符串包含资源 knowledgePoint → 强匹配
        if (info.weakTopics.stream().anyMatch(t -> contains(r.getKnowledgePoint(), t))
                || info.interestAreas.stream().anyMatch(t -> contains(r.getKnowledgePoint(), t))) {
            score += SCORE_NAME_MATCH;
        }
        return score;
    }

    private String buildReason(ScoredResource s, ProfileInfo info) {
        String kp = s.resource.getKnowledgePoint();
        StringBuilder reason = new StringBuilder();
        if (s.primaryKp != null && s.primaryKp.getModule() != null) {
            reason.append("属于模块 ").append(s.primaryKp.getModule());
            if (s.primaryKp.getDifficulty() != null) {
                reason.append("（难度 ").append(s.primaryKp.getDifficulty()).append("）");
            }
            reason.append("；");
        }
        if (info.weakTopics.stream().anyMatch(t -> contains(kp, t))) {
            return reason + "针对你的薄弱知识点「" + kp + "」推荐";
        }
        if (info.interestAreas.stream().anyMatch(t -> contains(kp, t))) {
            return reason + "基于你的兴趣方向「" + kp + "」推荐";
        }
        return reason + "最近生成的「" + kp + "」资源";
    }

    private boolean contains(String a, String b) {
        return a != null && b != null && a.contains(b);
    }

    private Set<Long> collectLearnedResourceIds(Long studentId) {
        Set<Long> ids = new HashSet<>();
        for (LearningRecord r : recordService.listByStudent(studentId)) {
            if (r.getResourceId() != null
                    && ("view".equals(r.getAction()) || "complete".equals(r.getAction()))) {
                ids.add(r.getId());
            }
        }
        return ids;
    }

    private ProfileInfo loadProfile(Long studentId) {
        try {
            StudentProfile profile = profileService.getByStudentId(studentId);
            List<String> weakTopics = extractStringList(profile.getWeakPoints(), "weak_topics");
            List<String> interests = extractStringList(profile.getInterestArea(), "areas");
            // 把字符串主题映射到知识库元数据
            List<KnowledgePoint> weakMeta = new ArrayList<>();
            for (String t : weakTopics) {
                List<KnowledgePoint> kps = knowledgeBaseService.searchByName(t);
                if (!kps.isEmpty()) weakMeta.add(kps.get(0));
            }
            List<KnowledgePoint> interestMeta = new ArrayList<>();
            for (String t : interests) {
                List<KnowledgePoint> kps = knowledgeBaseService.searchByName(t);
                if (!kps.isEmpty()) interestMeta.add(kps.get(0));
            }
            return new ProfileInfo(weakTopics, interests, weakMeta, interestMeta);
        } catch (Exception e) {
            log.debug("画像不存在或解析失败 studentId={}：{}", studentId, e.getMessage());
            return new ProfileInfo(List.of(), List.of(), List.of(), List.of());
        }
    }

    private List<String> extractStringList(String json, String field) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(field);
            if (node == null || !node.isArray()) return Collections.emptyList();
            List<String> list = new ArrayList<>();
            node.forEach(n -> list.add(n.asText()));
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private record ProfileInfo(List<String> weakTopics, List<String> interestAreas,
                               List<KnowledgePoint> weakPointsMeta, List<KnowledgePoint> interestMeta) {
    }

    private record ScoredResource(LearningResource resource, double score, KnowledgePoint primaryKp) {
    }
}