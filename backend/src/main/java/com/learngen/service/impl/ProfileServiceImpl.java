package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.StudentProfileMapper;
import com.learngen.model.StudentProfile;
import com.learngen.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 画像服务实现（增量更新版）。
 *
 * <p>对应 CLAUDE.md §10 / §11.3：
 * <ul>
 *   <li>构造器注入，Mapper 操作走 LambdaQueryWrapper</li>
 *   <li>6 维 JSON 字段按维度分别存储</li>
 *   <li>新增维度内容与已有内容「按字段深度合并」：对象字段递归合并；数组字段取并集去重</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    /** 6 个维度对应的 JSON 顶层 key（CLAUDE.md §4.1）。 */
    private static final Set<String> PROFILE_FIELDS = Set.of(
            "knowledge_base", "cognitive_style", "learning_goal",
            "weak_points", "learning_pace", "interest_area"
    );

    private final StudentProfileMapper profileMapper;
    private final Orchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final RedisCacheSupport cache;

    @Override
    public StudentProfile getByStudentId(Long studentId) {
        StudentProfile cached = cache.get(
                cache.key("profile", "byStudent", studentId), StudentProfile.class);
        if (cached != null) return cached;

        LambdaQueryWrapper<StudentProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentProfile::getStudentId, studentId)
                .orderByDesc(StudentProfile::getUpdatedAt)
                .last("LIMIT 1");
        StudentProfile profile = profileMapper.selectOne(wrapper);
        if (profile == null) {
            throw new BusinessException(404, "学生画像不存在：studentId=" + studentId);
        }
        cache.set(cache.key("profile", "byStudent", studentId),
                profile, RedisCacheSupport.TTL_PROFILE_BY_ID);
        return profile;
    }

    @Override
    public StudentProfile upsertFromAgent(Long studentId, String conversationContext) {
        // 1. 调 ProfileAgent 抽取画像
        AgentMessage result = orchestrator.dispatch("profile", AgentMessage.builder()
                .studentId(studentId)
                .role("user")
                .content(conversationContext)
                .build());

        String payload = result.getPayload() == null ? "{}" : result.getPayload();
        ParsedProfile parsed = parseProfileJson(payload);

        // 2. 取出已有画像做增量合并
        LambdaQueryWrapper<StudentProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentProfile::getStudentId, studentId)
                .orderByDesc(StudentProfile::getUpdatedAt)
                .last("LIMIT 1");
        StudentProfile existing = profileMapper.selectOne(wrapper);

        StudentProfile target = existing != null ? existing : new StudentProfile();
        target.setStudentId(studentId);

        // 3. 6 维分别合并
        target.setKnowledgeBase(mergeField(existing, "knowledge_base", parsed.knowledgeBase));
        target.setCognitiveStyle(mergeField(existing, "cognitive_style", parsed.cognitiveStyle));
        target.setLearningGoal(mergeField(existing, "learning_goal", parsed.learningGoal));
        target.setWeakPoints(mergeField(existing, "weak_points", parsed.weakPoints));
        target.setLearningPace(mergeField(existing, "learning_pace", parsed.learningPace));
        target.setInterestArea(mergeField(existing, "interest_area", parsed.interestArea));

        target.setUpdatedAt(LocalDateTime.now());

        // 4. 持久化
        if (existing == null) {
            profileMapper.insert(target);
            log.info("画像创建 studentId={} id={}", studentId, target.getId());
        } else {
            profileMapper.updateById(target);
            log.info("画像更新 studentId={} id={}", studentId, target.getId());
        }
        // 写后失效画像缓存，下次 getByStudentId 回源 DB
        cache.delete(cache.key("profile", "byStudent", studentId));
        return target;
    }

    /**
     * 增量合并策略：
     * <ul>
     *   <li>已有为空 → 直接使用新值</li>
     *   <li>已有为对象、新值为对象 → 深度合并（后者覆盖前者同名字段）</li>
     *   <li>已有为数组 → 与新数组取并集去重</li>
     *   <li>其他 → 后者覆盖前者</li>
     * </ul>
     */
    private String mergeField(StudentProfile existing, String field, String newJson) {
        String oldJson = switch (field) {
            case "knowledge_base" -> existing == null ? null : existing.getKnowledgeBase();
            case "cognitive_style" -> existing == null ? null : existing.getCognitiveStyle();
            case "learning_goal" -> existing == null ? null : existing.getLearningGoal();
            case "weak_points" -> existing == null ? null : existing.getWeakPoints();
            case "learning_pace" -> existing == null ? null : existing.getLearningPace();
            case "interest_area" -> existing == null ? null : existing.getInterestArea();
            default -> null;
        };
        if (oldJson == null || oldJson.isBlank() || oldJson.equals("{}")) {
            return newJson;
        }
        if (newJson == null || newJson.isBlank() || newJson.equals("{}")) {
            return oldJson;
        }
        try {
            JsonNode oldNode = objectMapper.readTree(oldJson);
            JsonNode newNode = objectMapper.readTree(newJson);
            return objectMapper.writeValueAsString(deepMerge(oldNode, newNode));
        } catch (Exception e) {
            log.warn("合并画像字段失败 field={}，使用新值：{}", field, e.getMessage());
            return newJson;
        }
    }

    /** 深度合并：对象递归合并；数组并集；其他后者覆盖。 */
    private JsonNode deepMerge(JsonNode a, JsonNode b) {
        if (!a.isObject() || !b.isObject()) {
            return b.isArray() ? mergeArray(a, b) : b;
        }
        ObjectNode merged = ((ObjectNode) a).deepCopy();
        Iterator<String> fields = b.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            JsonNode bVal = b.get(name);
            JsonNode aVal = merged.get(name);
            if (aVal == null) {
                merged.set(name, bVal);
            } else if (aVal.isArray() && bVal.isArray()) {
                merged.set(name, mergeArray(aVal, bVal));
            } else if (aVal.isObject() && bVal.isObject()) {
                merged.set(name, deepMerge(aVal, bVal));
            } else {
                merged.set(name, bVal);
            }
        }
        return merged;
    }

    /** 数组并集去重（按元素的 toString 文本）。 */
    private JsonNode mergeArray(JsonNode a, JsonNode b) {
        Set<String> seen = new LinkedHashSet<>();
        var arr = objectMapper.createArrayNode();
        for (JsonNode n : a) {
            if (seen.add(n.asText())) {
                arr.add(n);
            }
        }
        for (JsonNode n : b) {
            if (seen.add(n.asText())) {
                arr.add(n);
            }
        }
        return arr;
    }

    /** 解析 Agent 输出为 6 维 JSON 字符串。失败时各字段置为 {@code "{}"}。 */
    private ParsedProfile parseProfileJson(String payload) {
        ParsedProfile p = new ParsedProfile();
        try {
            String trimmed = stripCodeFence(payload);
            JsonNode root = objectMapper.readTree(trimmed);
            for (String field : PROFILE_FIELDS) {
                JsonNode val = root.get(field);
                String json = (val == null || val.isNull()) ? "{}" : objectMapper.writeValueAsString(val);
                switch (field) {
                    case "knowledge_base" -> p.knowledgeBase = json;
                    case "cognitive_style" -> p.cognitiveStyle = json;
                    case "learning_goal" -> p.learningGoal = json;
                    case "weak_points" -> p.weakPoints = json;
                    case "learning_pace" -> p.learningPace = json;
                    case "interest_area" -> p.interestArea = json;
                }
            }
        } catch (Exception e) {
            log.warn("解析 Agent 画像输出失败，使用空对象：{}", e.getMessage());
            p.knowledgeBase = "{}";
            p.cognitiveStyle = "{}";
            p.learningGoal = "{}";
            p.weakPoints = "{}";
            p.learningPace = "{}";
            p.interestArea = "{}";
        }
        return p;
    }

    private String stripCodeFence(String raw) {
        if (raw == null) return "{}";
        String t = raw.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        return t.isEmpty() ? "{}" : t;
    }

    /** 内部承载 6 维 JSON 字符串。 */
    private static class ParsedProfile {
        String knowledgeBase;
        String cognitiveStyle;
        String learningGoal;
        String weakPoints;
        String learningPace;
        String interestArea;
    }
}