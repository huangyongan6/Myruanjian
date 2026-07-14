package com.learngen.controller;

import com.learngen.model.ChatMessage;
import com.learngen.model.common.Result;
import com.learngen.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话 REST Controller（同步入口，便于测试）。
 *
 * <p>对应 CLAUDE.md §9.3：Controller 只做参数接收、调用 Service、返回结果。
 *
 * <p>WebSocket 流式入口见 {@link ChatWebSocketController}。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "对话", description = "对话消息发送与历史查询")
public class ChatRestController {

    private final ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "发送对话（同步）", description = "意图识别 → Agent 路由 → 返回结果")
    public Result<ChatResponse> send(@RequestBody ChatRequest request) {
        log.debug("REST 聊天 studentId={}", request.getStudentId());
        String payload = chatService.handleMessage(request.getStudentId(), request.getContent());
        ChatResponse response = new ChatResponse();
        response.setType("message");
        response.setContent(payload);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        return Result.success(response);
    }

    /**
     * 对话历史查询（基于游标的向上加载）。
     *
     * @param studentId 学生 ID
     * @param limit     最多返回条数（默认 50，上限 200）
     * @param before    可选游标，ISO-8601 时间字符串；传入时仅返回 createdAt 严格早于该时间的消息
     */
    @GetMapping("/history/{studentId}")
    @Operation(summary = "查询对话历史", description = "按时间倒序，最多返回 limit 条；传入 before 时仅返回更早的消息（用于向上加载）")
    public Result<List<ChatMessage>> history(@PathVariable Long studentId,
                                             @RequestParam(defaultValue = "50") int limit,
                                             @RequestParam(required = false) String before) {
        return Result.success(chatService.history(studentId, limit, before));
    }

    @Data
    public static class ChatRequest {
        private Long studentId;
        private String content;
    }

    @Data
    public static class ChatResponse {
        private String type;
        private String content;
        private String timestamp;
    }
}