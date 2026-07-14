package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习画像实体（6 维度，JSON 存储）。
 *
 * <p>对应表 {@code student_profile}。6 个 JSON 字段以 {@code String} 类型存储，
 * 序列化 / 反序列化由 Service 层负责。
 */
@Data
@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 ID */
    private Long studentId;

    /** 知识基础：{math_level, programming_level, ml_familiarity} */
    private String knowledgeBase;

    /** 认知风格：{visual, textual, hands_on} */
    private String cognitiveStyle;

    /** 学习目标：{goal_type, target_direction} */
    private String learningGoal;

    /** 易错点：{weak_topics[], mistake_types[]} */
    private String weakPoints;

    /** 学习节奏：{daily_hours, pace} */
    private String learningPace;

    /** 兴趣方向：{areas[], preferred_project_type} */
    private String interestArea;

    private LocalDateTime updatedAt;
}