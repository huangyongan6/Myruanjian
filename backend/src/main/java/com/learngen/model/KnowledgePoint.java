package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点实体（机器学习知识库使用）。
 *
 * <p>对应表 {@code knowledge_point}。本表存储 6 大模块的知识点元数据，
 * 详情 Markdown 文件存放在 {@code resources/knowledge/} 目录，
 * 路径由 {@code contentPath} 指向。
 */
@Data
@TableName("knowledge_point")
public class KnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块编号：1~6（基础概念 / 经典算法 / 无监督 / 深度学习 / 实践工具 / 项目实战） */
    private Integer module;

    /** 知识点名称 */
    private String name;

    /** 描述（一句话） */
    private String description;

    /** 关联 Markdown 文件路径（相对 resources/knowledge） */
    private String contentPath;

    /** 难度：easy / medium / hard */
    private String difficulty;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}