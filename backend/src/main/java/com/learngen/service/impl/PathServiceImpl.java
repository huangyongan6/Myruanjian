package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.LearningPathMapper;
import com.learngen.model.KnowledgePoint;
import com.learngen.model.LearningPath;
import com.learngen.model.LearningRecord;
import com.learngen.model.StudentProfile;
import com.learngen.service.KnowledgeBaseService;
import com.learngen.service.LearningRecordService;
import com.learngen.service.PathService;
import com.learngen.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路径服务实现。
 *
 * <p>对应 CLAUDE.md §4.3 PathAgent。
 *
 * <p>知识库接入（C1）：构造 PathAgent 输入时，先调用
 * {@link KnowledgeBaseService#listByModule(Integer)} 把 6 个模块的全部知识点元数据
 * 拼成"知识库图谱摘要"注入 {@code context.knowledge_preview}。PathAgent 继承
 * {@link com.learngen.agent.TextOutputAgent}，会自动消费该字段（A 方案覆盖）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PathServiceImpl implements PathService {

    private final Orchestrator orchestrator;
    private final LearningPathMapper pathMapper;
    private final ProfileService profileService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final LearningRecordService recordService;

    /** 解析 steps 数量（用正则粗略提取，便于先存元数据）。 */
    private static final Pattern STEPS_PATTERN = Pattern.compile("\"step\"\\s*:\\s*(\\d+)");

    /** 注入 PathAgent 的知识库图谱摘要字符上限，避免 prompt 过长。 */
    private static final int KNOWLEDGE_PREVIEW_MAX = 1800;

    @Override
    public LearningPath generate(Long studentId) {
        StudentProfile profile;
        try {
            profile = profileService.getByStudentId(studentId);
        } catch (BusinessException e) {
            throw new BusinessException(400, "请先完成画像构建再生成路径");
        }

        // 获取学生学习历史（已完成的动作和资源）
        List<LearningRecord> history = recordService.listByStudent(studentId);
        // 获取已有的学习路径（当前进度）
        LearningPath existingPath = null;
        try {
            existingPath = getLatest(studentId);
        } catch (BusinessException e) {
            // 没有旧路径，第一次生成
        }

        // C1：拉取知识库 6 模块清单，拼成图谱摘要注入 context。
        // A：context.knowledge_preview 会被 TextOutputAgent.buildUserInput 自动拼到 user prompt。
        Map<String, Object> context = buildKnowledgeContext();

        // 把画像 JSON 序列化 + 学习历史作为 content，便于模型读取
        AgentMessage result = orchestrator.dispatch("path", AgentMessage.builder()
                .studentId(studentId)
                .role("user")
                .content(buildContext(profile, history, existingPath))
                .context(context)
                .build());

        String payload = result.getPayload() == null ? "{\"steps\":[]}" : result.getPayload();
        int totalSteps = countSteps(payload);

        // upsert
        LambdaQueryWrapper<LearningPath> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningPath::getStudentId, studentId)
                .orderByDesc(LearningPath::getUpdatedAt)
                .last("LIMIT 1");
        LearningPath existing = pathMapper.selectOne(wrapper);

        LearningPath target = existing != null ? existing : new LearningPath();
        target.setStudentId(studentId);
        target.setTotalSteps(totalSteps);
        target.setPathData(payload);
        target.setCurrentStep(0); // 重新生成时重置进度
        target.setUpdatedAt(LocalDateTime.now());
        if (existing == null) {
            pathMapper.insert(target);
            log.info("学习路径创建 studentId={} id={}", studentId, target.getId());
        } else {
            pathMapper.updateById(target);
            log.info("学习路径更新 studentId={} id={}", studentId, target.getId());
        }
        return target;
    }

    @Override
    public LearningPath getLatest(Long studentId) {
        LambdaQueryWrapper<LearningPath> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningPath::getStudentId, studentId)
                .orderByDesc(LearningPath::getUpdatedAt)
                .last("LIMIT 1");
        LearningPath path = pathMapper.selectOne(wrapper);
        if (path == null) {
            throw new BusinessException(404, "学习路径不存在：studentId=" + studentId);
        }
        return path;
    }

    @Override
    public LearningPath updateCurrentStep(Long studentId, int currentStep) {
        LearningPath path = getLatest(studentId);
        path.setCurrentStep(currentStep);
        path.setUpdatedAt(LocalDateTime.now());
        pathMapper.updateById(path);
        log.info("学习进度更新 studentId={} currentStep={}", studentId, currentStep);
        return path;
    }

    /**
     * 构造知识库图谱摘要：6 个模块的知识点清单（按 module 聚合），截断到指定长度。
     *
     * <p>为什么不传全文 Markdown：路径生成只需要"有哪些知识点、各属什么模块、难度如何"
     * 这类图谱信息，不需要每篇的代码示例；传图谱摘要既给模型足够事实，又避免 prompt 过长。
     */
    private Map<String, Object> buildKnowledgeContext() {
        Map<String, Object> context = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        sb.append("【机器学习知识库图谱】（请在规划学习路径时严格参考以下知识点清单与模块归属）\n\n");
        for (int module = 1; module <= 6; module++) {
            List<KnowledgePoint> points = knowledgeBaseService.listByModule(module);
            if (points.isEmpty()) continue;
            sb.append("模块 ").append(module).append("：\n");
            for (KnowledgePoint p : points) {
                sb.append("  - ").append(p.getName())
                  .append("（难度：").append(p.getDifficulty() == null ? "medium" : p.getDifficulty())
                  .append("，简介：").append(p.getDescription() == null ? "-" : p.getDescription())
                  .append("）\n");
            }
            sb.append('\n');
        }
        String preview = sb.toString();
        if (preview.length() > KNOWLEDGE_PREVIEW_MAX) {
            preview = preview.substring(0, KNOWLEDGE_PREVIEW_MAX) + "...";
        }
        context.put("knowledge_preview", preview);
        context.put("knowledge_query", "学习路径规划");
        log.debug("PathService 注入知识库图谱摘要 length={}", preview.length());
        return context;
    }

    /** 把画像序列化为 JSON 字符串传入 Agent 作为上下文。 */
    private String buildContext(StudentProfile profile, List<LearningRecord> history,
                                 LearningPath existingPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("学生画像：{");
        sb.append("\"knowledge_base\":").append(nullSafe(profile.getKnowledgeBase())).append(',');
        sb.append("\"cognitive_style\":").append(nullSafe(profile.getCognitiveStyle())).append(',');
        sb.append("\"learning_goal\":").append(nullSafe(profile.getLearningGoal())).append(',');
        sb.append("\"weak_points\":").append(nullSafe(profile.getWeakPoints())).append(',');
        sb.append("\"learning_pace\":").append(nullSafe(profile.getLearningPace())).append(',');
        sb.append("\"interest_area\":").append(nullSafe(profile.getInterestArea()));
        sb.append("}\n\n");

        // 加入学习历史记录
        if (!history.isEmpty()) {
            sb.append("学生学习历史（最近 ").append(Math.min(history.size(), 20)).append(" 条）：\n");
            // 按时间倒序取最近20条
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 20); i--) {
                LearningRecord r = history.get(i);
                sb.append("- 动作：").append(r.getAction());
                if (r.getResourceId() != null) sb.append("，资源ID：").append(r.getResourceId());
                if (r.getDuration() != null) sb.append("，时长：").append(r.getDuration()).append("秒");
                if (r.getScore() != null) sb.append("，得分：").append(r.getScore());
                if (r.getCreatedAt() != null) sb.append("，时间：").append(r.getCreatedAt());
                sb.append('\n');
            }
            sb.append('\n');
        }

        // 加入当前路径进度
        if (existingPath != null) {
            sb.append("当前学习路径进度：已完成 ").append(existingPath.getCurrentStep())
              .append(" / ").append(existingPath.getTotalSteps()).append(" 步\n");
            // 解析 pathData 中的 steps 标题
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode pathData = mapper.readTree(existingPath.getPathData());
                com.fasterxml.jackson.databind.JsonNode steps = pathData.get("steps");
                if (steps != null && steps.isArray()) {
                    sb.append("已完成步骤：");
                    for (int i = 0; i < Math.min(existingPath.getCurrentStep(), steps.size()); i++) {
                        com.fasterxml.jackson.databind.JsonNode step = steps.get(i);
                        String title = step.has("title") ? step.get("title").asText() : ("步骤" + (i + 1));
                        sb.append("[").append(title).append("] ");
                    }
                    sb.append("\n待学习步骤：");
                    for (int i = existingPath.getCurrentStep(); i < steps.size(); i++) {
                        com.fasterxml.jackson.databind.JsonNode step = steps.get(i);
                        String title = step.has("title") ? step.get("title").asText() : ("步骤" + (i + 1));
                        sb.append("[").append(title).append("] ");
                    }
                    sb.append('\n');
                }
            } catch (Exception e) {
                log.debug("解析已有路径失败：{}", e.getMessage());
            }
        }
        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null || s.isBlank() ? "{}" : s;
    }

    private int countSteps(String payload) {
        Matcher matcher = STEPS_PATTERN.matcher(payload);
        int max = 0;
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            if (n > max) max = n;
        }
        return max;
    }
}