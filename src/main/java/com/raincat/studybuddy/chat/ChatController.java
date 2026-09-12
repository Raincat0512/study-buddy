package com.raincat.studybuddy.chat;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    public record ChatRequest(@NotBlank String sessionId, @NotBlank String message) {
    }

    /** 单轮问答（RAG 上下文 + agent 工具 + 多轮历史） */
    @PostMapping
    public ChatService.ChatAnswer ask(@RequestBody ChatRequest request) {
        return chatService.ask(request.sessionId(), request.message());
    }

    /** 拉取某个会话的历史消息 */
    @GetMapping("/history")
    public List<ChatService.ChatItem> history(@RequestParam String sessionId) {
        return chatService.history(sessionId);
    }

    /** 清空会话（相当于"开新对话"） */
    @DeleteMapping
    public void reset(@RequestParam String sessionId) {
        chatService.reset(sessionId);
    }
}
