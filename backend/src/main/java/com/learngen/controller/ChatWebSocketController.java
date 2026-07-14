package com.learngen.controller;

import com.learngen.model.common.Result;
import com.learngen.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * 对话 WebSocket Controller（STOMP 流式入口）。
 *
 * <p>对应 CLAUDE.md §13.1 / §22 消息结构：
 * <pre>
 * { "type": "message|progress|resource|error|done",
 *   "content": "...",
 *   "timestamp": "ISO 字符串" }
 * </pre>
 *
 * <p>前端连接地址：{@code ws://localhost:8080/ws}
 * <br>客户端发送到：{@code /app/chat.send}
 * <br>服务端推送到：{@code /topic/chat/{studentId}}
 *
 * <p>本 Controller 仅做参数接收 + Service 调度 + 推送，业务逻辑全部在
 * ChatService 中（CLAUDE.md §10.3）。每收到一个 AI chunk 立即推送一条
 * {@code type=message} 帧，实现打字机效果；流结束时推送 {@code type=done}。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void handleStream(@Payload ChatRequest request) {
        Long studentId = request.getStudentId();
        String content = request.getContent();
        log.info("WebSocket 聊天 studentId={} contentLen={}",
                studentId, content == null ? 0 : content.length());

        chatService.handleMessageStream(
                studentId,
                content,
                // onChunk：每片推一条 type=message
                chunk -> push(studentId, "message", chunk),
                // onDone：流结束推 type=done
                () -> push(studentId, "done", null),
                // onError：异常推 type=error
                err -> push(studentId, "error",
                        err == null ? "未知异常" : err.getMessage()));
    }

    /** 统一的推送封装：避免三处推送代码重复。 */
    private void push(Long studentId, String type, String content) {
        ChatResponse response = new ChatResponse();
        response.setType(type);
        response.setContent(content);
        response.setTimestamp(LocalDateTime.now().toString());
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + studentId, response);
        } catch (Exception e) {
            // 推送失败仅记日志，不影响 Agent 流继续（与 TextOutputAgent 容错一致）
            log.warn("WebSocket 推送失败 studentId={} type={} err={}",
                    studentId, type, e.getMessage());
        }
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