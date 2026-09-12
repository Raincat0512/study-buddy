package com.raincat.studybuddy.chat;

import com.raincat.studybuddy.config.AppProperties;
import com.raincat.studybuddy.document.RetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG + 工具调用的对话编排：
 * 每轮先做一次语义检索注入参考资料，再让模型带着工具集回答；
 * 历史消息由本项目自己存取（存 PG），不依赖框架记忆组件。
 */
@Service
public class ChatService {

    private static final String SYSTEM_TEMPLATE = """
            你是「学习助手」，帮助用户检索和理解他自己导入的学习资料。

            回答规则：
            1. 优先依据下面的【参考资料】回答学习内容问题，引用时在句末标注来源编号，例如 [1]。
            2. 如果资料中没有相关内容，或问题与学习资料无关，明确说"资料库中没有相关内容"，不要编造。
            3. 需要浏览资料概况（有哪些资料、有哪些标签）时，可以调用提供的工具。
            4. 用简洁的中文回答，必要时使用要点列表。

            【参考资料】
            %s
            """;

    private final ChatClient chatClient;
    private final RetrievalService retrievalService;
    private final ChatMessageRepository messageRepository;
    private final NoteTools noteTools;
    private final AppProperties props;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       RetrievalService retrievalService,
                       ChatMessageRepository messageRepository,
                       NoteTools noteTools,
                       AppProperties props) {
        this.chatClient = chatClientBuilder.build();
        this.retrievalService = retrievalService;
        this.messageRepository = messageRepository;
        this.noteTools = noteTools;
        this.props = props;
    }

    public record ChatAnswer(String answer) {
    }

    public record ChatItem(String role, String content, String createdAt) {
    }

    @Transactional
    public ChatAnswer ask(String sessionId, String question) {
        String answer = chatClient.prompt()
                .system(SYSTEM_TEMPLATE.formatted(retrieveContext(question)))
                .messages(loadHistory(sessionId))
                .user(question)
                .tools(noteTools)
                .call()
                .content();
        persistTurn(sessionId, question, answer);
        return new ChatAnswer(answer);
    }

    private String retrieveContext(String question) {
        return retrievalService.buildContext(retrievalService.retrieve(question));
    }

    private List<Message> loadHistory(String sessionId) {
        int limit = Math.max(0, props.getRag().getHistoryMessages());
        if (limit == 0) {
            return List.of();
        }
        // 取最近 N 条（倒序查再反转，保证时间正序）
        List<ChatMessageEntity> tail = messageRepository
                .findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, limit));
        List<Message> messages = new ArrayList<>();
        for (int i = tail.size() - 1; i >= 0; i--) {
            ChatMessageEntity m = tail.get(i);
            messages.add("user".equals(m.getRole())
                    ? new UserMessage(m.getContent())
                    : new AssistantMessage(m.getContent()));
        }
        return messages;
    }

    private void persistTurn(String sessionId, String question, String answer) {
        messageRepository.save(new ChatMessageEntity(sessionId, "user", question));
        messageRepository.save(new ChatMessageEntity(sessionId, "assistant", answer));
    }

    @Transactional(readOnly = true)
    public List<ChatItem> history(String sessionId) {
        return messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(0, 1_000))
                .stream()
                .map(m -> new ChatItem(m.getRole(), m.getContent(), m.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public void reset(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
    }
}
