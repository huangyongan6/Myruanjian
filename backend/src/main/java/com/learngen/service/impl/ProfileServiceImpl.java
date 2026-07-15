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
import com.learngen.model.ChatMessage;
import com.learngen.model.LearningRecord;
import com.learngen.model.LearningResource;
import com.learngen.model.StudentProfile;
import com.learngen.service.ChatService;
import com.learngen.service.LearningRecordService;
import com.learngen.service.PathService;
import com.learngen.service.ProfileService;
import com.learngen.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final ChatService chatService;
    private final LearningRecordService recordService;
    private final ObjectProvider<PathService> pathServiceProvider;
    private final ObjectProvider<ResourceService> resourceServiceProvider;

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
        // 1. 构建综合上下文：对话历史 + 学习记录统计
        String fullContext = buildProfileContext(studentId, conversationContext);

        // 2. 调 ProfileAgent 抽取画像
        AgentMessage result = orchestrator.dispatch("profile", AgentMessage.builder()
                .studentId(studentId)
                .role("user")
                .content(fullContext)
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

    /**
     * 构建画像生成的综合上下文：整合对话历史 + 学习记录 + 学习路线数据。
     *
     * <p>让 ProfileAgent 基于真实学习行为数据（而非仅对话）来推断学生画像，
     * 使画像更准确、更具客观性。
     */
    private String buildProfileContext(Long studentId, String conversationContext) {
        StringBuilder sb = new StringBuilder();

        // 1. 对话历史摘要（最近 20 条）
        List<ChatMessage> recentChats = chatService.history(studentId, 20, null);
        if (!recentChats.isEmpty()) {
            sb.append("【对话历史摘要】（最近的 ").append(recentChats.size()).append(" 条对话）\n");
            for (ChatMessage msg : recentChats) {
                String role = "user".equals(msg.getRole()) ? "学生" : "助手";
                String content = msg.getContent();
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append("- ").append(role).append("：").append(content).append("\n");
            }
            sb.append("\n");
        }

        // 2. 学习记录统计（资源中心学习情况）
        Map<String, Object> evaluation = recordService.evaluate(studentId);
        if (evaluation != null && !evaluation.isEmpty()) {
            sb.append("【资源中心学习统计】\n");
            int viewCount = ((Number) evaluation.getOrDefault("viewCount", 0)).intValue();
            int completeCount = ((Number) evaluation.getOrDefault("completeCount", 0)).intValue();
            int quizCount = ((Number) evaluation.getOrDefault("quizCount", 0)).intValue();
            Object avgScore = evaluation.get("averageScore");
            double completionRate = ((Number) evaluation.getOrDefault("completionRate", 0)).doubleValue();
            Object totalDuration = evaluation.get("totalDurationSeconds");

            sb.append("- 浏览资源数：").append(viewCount).append("\n");
            sb.append("- 完成资源数：").append(completeCount).append("\n");
            sb.append("- 答题数：").append(quizCount).append("\n");
            sb.append("- 答题平均分：").append(avgScore != null ? String.format("%.1f", avgScore) : "无").append("\n");
            sb.append("- 完成率：").append(String.format("%.1f%%", completionRate * 100)).append("\n");
            if (totalDuration != null) {
                int totalSeconds = ((Number) totalDuration).intValue();
                sb.append("- 累计学习时长：").append(totalSeconds / 60).append(" 分钟\n");

                // 计算日均学习时长 = 累计时长 / 学习天数
                var records = recordService.listByStudent(studentId);
                if (!records.isEmpty()) {
                    // 获取有学习记录的首尾日期
                    LearningRecord first = records.get(records.size() - 1);  // 按时间倒序，最后一条是最早的
                    LearningRecord last = records.get(0);  // 第一条是最新的

                    if (first.getCreatedAt() != null && last.getCreatedAt() != null) {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                            first.getCreatedAt().toLocalDate(),
                            last.getCreatedAt().toLocalDate()) + 1;  // +1 包含首尾两天

                        if (daysBetween > 0) {
                            double avgMinutes = (totalSeconds / 60.0) / daysBetween;
                            sb.append("- 日均学习时长：").append(String.format("%.1f", avgMinutes)).append(" 分钟/天\n");
                            sb.append("  （基于 ").append(daysBetween).append(" 天的学习记录）\n");
                        }
                    }
                }
            }
            // 分析推断建议
            sb.append("\n  → 推断参考：\n");
            if (avgScore != null) {
                double score = ((Number) avgScore).doubleValue();
                if (score >= 80) {
                    sb.append("    答题表现优秀，知识掌握较好\n");
                } else if (score >= 60) {
                    sb.append("    答题表现一般，存在薄弱环节\n");
                } else {
                    sb.append("    答题表现较差，建议加强基础知识\n");
                }
            }
            if (completionRate >= 0.7) {
                sb.append("    学习完成率高，学习态度认真\n");
            } else if (completionRate >= 0.4) {
                sb.append("    学习完成率中等，可适当调整节奏\n");
            } else if (viewCount > 0) {
                sb.append("    学习完成率偏低，可能需要更简单的起点\n");
            }
            sb.append("\n");
        }

        // 3. 答题薄弱知识点分析（基于 Quiz 资源得分统计）
        try {
            ResourceService resourceService = resourceServiceProvider.getObject();
            List<LearningRecord> allRecords = recordService.listByStudent(studentId);
            // 按知识点聚合答题分数
            Map<String, List<Integer>> quizByKpoint = new java.util.HashMap<>();
            for (LearningRecord r : allRecords) {
                if ("quiz".equals(r.getAction()) && r.getResourceId() != null && r.getScore() != null) {
                    LearningResource res = resourceService.getById(r.getResourceId());
                    if (res != null && res.getKnowledgePoint() != null) {
                        quizByKpoint.computeIfAbsent(res.getKnowledgePoint(), k -> new java.util.ArrayList<>()).add(r.getScore());
                    }
                }
            }
            if (!quizByKpoint.isEmpty()) {
                sb.append("【答题薄弱知识点分析】（基于每次 quiz 得分）\n");
                // 计算每个知识点的平均分，按升序排列（最低的在前 = 最薄弱）
                List<Map.Entry<String, Double>> sorted = new java.util.ArrayList<>();
                for (var entry : quizByKpoint.entrySet()) {
                    double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                    sorted.add(Map.entry(entry.getKey(), avg));
                }
                sorted.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
                int rank = 1;
                for (var entry : sorted) {
                    String kp = entry.getKey();
                    double avg = entry.getValue();
                    String level = avg >= 80 ? "掌握良好" : avg >= 60 ? "一般" : "薄弱";
                    sb.append(String.format("  %d. %s（均分 %.1f） - %s\n", rank++, kp, avg, level));
                }
                // 标记薄弱知识点（平均分 < 70）
                List<String> weakList = sorted.stream()
                        .filter(e -> e.getValue() < 70)
                        .map(Map.Entry::getKey)
                        .toList();
                if (!weakList.isEmpty()) {
                    sb.append("  → 建议重点复习：").append(String.join("、", weakList)).append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.debug("答题薄弱知识点分析失败 studentId={}: {}", studentId, e.getMessage());
        }

        // 4. 学习路线统计
        try {
            PathService pathService = pathServiceProvider.getObject();
            var path = pathService.getLatest(studentId);
            if (path != null) {
                sb.append("【学习路线情况】\n");
                sb.append("- 路线总步数：").append(path.getTotalSteps()).append("\n");
                sb.append("- 当前进度：第 ").append(path.getCurrentStep()).append(" 步\n");
                double progress = path.getTotalSteps() > 0
                        ? (path.getCurrentStep() * 100.0 / path.getTotalSteps()) : 0;
                sb.append("- 完成进度：").append(String.format("%.1f%%", progress)).append("\n");
                if (progress > 50) {
                    sb.append("  → 学习进度良好，坚持即可完成\n");
                } else if (progress > 0) {
                    sb.append("  → 学习进度较慢，建议调整学习计划\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.debug("获取学习路线失败 studentId={}: {}", studentId, e.getMessage());
        }

        // 4. 外部传入的对话上下文（可选）
        if (conversationContext != null && !conversationContext.isBlank()) {
            sb.append("【本次对话内容】\n").append(conversationContext).append("\n");
        }

        return sb.toString();
    }
}