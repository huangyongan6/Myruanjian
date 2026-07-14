package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习路径实体。
 *
 * <p>对应表 {@code learning_path}。{@code pathData} 为 JSON 字符串，包含 steps 列表。
 */
@Data
@TableName("learning_path")
public class LearningPath {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 ID */
    private Long studentId;

    /** 总步数 */
    private Integer totalSteps;

    /** 当前步数 */
    private Integer currentStep;

    /** 路径数据（JSON 字符串） */
    private String pathData;

    private LocalDateTime updatedAt;
}