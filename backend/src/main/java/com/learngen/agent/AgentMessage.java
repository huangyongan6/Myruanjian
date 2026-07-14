package com.learngen.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 间传递的消息体。
 *
 * <p>对应 CLAUDE.md §4.1 agent/AgentMessage。所有 Agent 输入输出统一为此结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    /** Agent 名称（发起方或目标方） */
    private String agentName;

    /** 消息角色：user / system / assistant */
    private String role;

    /** 文本内容 */
    private String content;

    /** 关联学生 ID（对话场景必填） */
    private Long studentId;

    /** 关联知识点（资源生成场景必填） */
    private String knowledgePoint;

    /** 难度：easy / medium / hard */
    private String difficulty;

    /** 附加上下文（如已有画像 JSON、当前学习进度等） */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /** Agent 输出（Markdown / JSON / 列表 等原始字符串） */
    private String payload;
}