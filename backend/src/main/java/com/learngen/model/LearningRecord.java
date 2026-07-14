package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录实体（效果评估用）。
 *
 * <p>对应表 {@code learning_record}。action 取值：view / complete / quiz。
 */
@Data
@TableName("learning_record")
public class LearningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 ID */
    private Long studentId;

    /** 资源 ID（可空） */
    private Long resourceId;

    /** 行为：view / complete / quiz */
    private String action;

    /** 分数（quiz 时使用，可空） */
    private Integer score;

    /** 学习时长（秒，可空） */
    private Integer duration;

    private LocalDateTime createdAt;
}