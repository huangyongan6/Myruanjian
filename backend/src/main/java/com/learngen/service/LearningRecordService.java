package com.learngen.service;

import com.learngen.model.LearningRecord;

import java.util.List;
import java.util.Map;

/**
 * 学习记录服务接口（效果评估用）。
 *
 * <p>对应 CLAUDE.md §4.1 service/LearningRecordService 与 §21 加分项「学习效果评估」。
 */
public interface LearningRecordService {

    /**
     * 记录一次学习行为。
     *
     * @param studentId  学生 ID
     * @param resourceId 资源 ID（quiz 类行为可空）
     * @param action     行为：view / complete / quiz
     * @param score      分数（quiz 时使用，可空）
     * @param duration   学习时长（秒，可空）
     */
    LearningRecord record(Long studentId, Long resourceId, String action,
                          Integer score, Integer duration);

    /** 查询某学生的所有学习记录，按时间倒序。 */
    List<LearningRecord> listByStudent(Long studentId);

    /**
     * 学习效果评估报告。
     *
     * <p>对应 CLAUDE.md §21 加分项：根据学习记录生成统计指标，
     * 供 PathAgent 动态调整路径使用。
     *
     * @return 报告 Map（key 为指标名，value 为数值或字符串）
     */
    Map<String, Object> evaluate(Long studentId);

    /**
     * 获取当天累计学习时长（秒）。
     *
     * @param studentId 学生 ID
     * @return 当天学习时长（秒），无记录时返回 0
     */
    int getTodayDuration(Long studentId);
}