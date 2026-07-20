package com.learngen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.LearningRecordMapper;
import com.learngen.model.LearningRecord;
import com.learngen.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习记录服务实现。
 *
 * <p>对应 CLAUDE.md §10 / §11.3：构造器注入；Mapper 操作走 LambdaQueryWrapper。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private static final Set<String> ALLOWED_ACTIONS = Set.of("view", "complete", "quiz");

    /** 学习记录变更广播目的地前缀 */
    static final String RECORD_TOPIC_PREFIX = "/topic/records/";

    private final LearningRecordMapper recordMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public LearningRecord record(Long studentId, Long resourceId, String action,
                                 Integer score, Integer duration) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new BusinessException(400,
                    "action 必须是 view / complete / quiz 之一，当前：" + action);
        }

        LearningRecord record = new LearningRecord();
        record.setStudentId(studentId);
        record.setResourceId(resourceId);
        record.setAction(action);
        record.setScore(score);
        record.setDuration(duration);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        log.debug("学习记录 studentId={} action={} resourceId={} score={}",
                studentId, action, resourceId, score);

        // 广播学习记录变更事件：Dashboard / 其他页面实时刷新
        publishRecordEvent(studentId, action, record);

        return record;
    }

    /**
     * 广播学习记录变更事件，失败仅记日志不影响主流程。
     *
     * <p>前端 Dashboard 订阅 {@code /topic/records/{studentId}} 收到后
     * 立即重新拉取 record + evaluate，保证数字、图表、报告联动更新。
     */
    private void publishRecordEvent(Long studentId, String action, LearningRecord record) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "record");
            event.put("action", action);
            event.put("studentId", studentId);
            event.put("resourceId", record.getResourceId());
            event.put("score", record.getScore());
            event.put("duration", record.getDuration());
            event.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend(RECORD_TOPIC_PREFIX + studentId, event);
        } catch (Exception e) {
            // 推送失败仅记日志：埋点不阻断主流程
            log.warn("广播学习记录事件失败 studentId={} action={} err={}",
                    studentId, action, e.getMessage());
        }
    }

    @Override
    public List<LearningRecord> listByStudent(Long studentId) {
        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getStudentId, studentId)
                .orderByDesc(LearningRecord::getCreatedAt);
        return recordMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> evaluate(Long studentId) {
        List<LearningRecord> records = listByStudent(studentId);

        Map<String, Object> report = new HashMap<>();
        report.put("studentId", studentId);
        report.put("totalRecords", records.size());

        if (records.isEmpty()) {
            report.put("viewCount", 0);
            report.put("completeCount", 0);
            report.put("quizCount", 0);
            report.put("averageScore", null);
            report.put("totalDurationSeconds", 0);
            report.put("weakTopics", List.of());
            return report;
        }

        int viewCount = 0;
        int completeCount = 0;
        int quizCount = 0;
        long totalScore = 0;
        long totalDuration = 0;
        int scoredQuizzes = 0;

        for (LearningRecord r : records) {
            switch (r.getAction()) {
                case "view" -> {
                    viewCount++;
                }
                case "complete" -> completeCount++;
                case "quiz" -> {
                    quizCount++;
                    if (r.getScore() != null) {
                        totalScore += r.getScore();
                        scoredQuizzes++;
                    }
                }
                default -> { /* 忽略未知 action */ }
            }
            if (r.getDuration() != null && r.getDuration() > 0) {
                totalDuration += r.getDuration();
            }
        }
        report.put("viewCount", viewCount);
        report.put("completeCount", completeCount);
        report.put("quizCount", quizCount);
        report.put("averageScore", scoredQuizzes == 0 ? null
                : (double) totalScore / scoredQuizzes);
        report.put("totalDurationSeconds", totalDuration);

        // 完成率（CLAUDE.md §21 加分项：动态调整）
        double completionRate = (viewCount + completeCount) == 0
                ? 0.0
                : (double) completeCount / (viewCount + completeCount);
        report.put("completionRate", completionRate);

        log.info("学习评估 studentId={} 完成率={} 平均分={}",
                studentId, completionRate, report.get("averageScore"));
        return report;
    }

    @Override
    public int getTodayDuration(Long studentId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        LambdaQueryWrapper<LearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningRecord::getStudentId, studentId)
                .ge(LearningRecord::getCreatedAt, startOfDay)
                .lt(LearningRecord::getCreatedAt, endOfDay);
        List<LearningRecord> todayRecords = recordMapper.selectList(wrapper);

        int total = 0;
        for (LearningRecord r : todayRecords) {
            if (r.getDuration() != null && r.getDuration() > 0) {
                total += r.getDuration();
            }
        }
        log.debug("当日学习时长 studentId={} duration={} 秒", studentId, total);
        return total;
    }
}