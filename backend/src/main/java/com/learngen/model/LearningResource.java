package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习资源实体（4 种类型，content 为 JSON）。
 *
 * <p>对应表 {@code learning_resource}。type 取值：doc / quiz / reading / code。
 */
@Data
@TableName("learning_resource")
public class LearningResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 ID */
    private Long studentId;

    /** 资源类型：doc / quiz / reading / code */
    private String type;

    /** 资源标题 */
    private String title;

    /** 资源内容（JSON 字符串） */
    private String content;

    /** 关联知识点（可空） */
    private String knowledgePoint;

    /** 难度：easy / medium / hard，默认 medium */
    private String difficulty;

    private LocalDateTime createdAt;
}