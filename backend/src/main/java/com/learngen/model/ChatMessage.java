package com.learngen.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体。
 *
 * <p>对应表 {@code chat_message}。
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 ID */
    private Long studentId;

    /** 角色：user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 响应 Agent 类型（可空） */
    private String agentType;

    /** 逻辑删除标记（true = 已删除，不查询） */
    private Boolean deleted = false;

    private LocalDateTime createdAt;
}