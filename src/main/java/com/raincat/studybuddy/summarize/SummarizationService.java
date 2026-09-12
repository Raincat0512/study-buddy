package com.raincat.studybuddy.summarize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** 导入时用 LLM 生成摘要与标签（结构化输出）。失败时降级为"未分类"，不阻塞导入 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    /** 只把原文开头部分给 LLM，控制 token 成本 */
    private static final int MAX_PROMPT_CHARS = 6_000;

    private final ChatClient chatClient;

    public SummarizationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public SummaryAndTags summarize(String title, String content) {
        String excerpt = content.length() <= MAX_PROMPT_CHARS
                ? content
                : content.substring(0, MAX_PROMPT_CHARS) + "\n……（已截断）";
        String user = """
                请为下面这篇学习资料生成摘要和标签。

                要求：
                1. summary：80 字以内的中文摘要，说明资料讲了什么、适合什么场景。
                2. tags：3 到 6 个中文标签，用于后续分类检索，如「操作系统」「面试」「笔记」。

                资料标题：%s
                资料内容：
                %s
                """.formatted(title, excerpt);
        try {
            SummaryAndTags result = chatClient.prompt()
                    .user(user)
                    .call()
                    .entity(SummaryAndTags.class);
            if (result == null || result.summary() == null || result.summary().isBlank()) {
                return fallback(title);
            }
            return result;
        } catch (Exception e) {
            log.warn("摘要生成失败，降级为未分类：{}", e.getMessage());
            return fallback(title);
        }
    }

    private SummaryAndTags fallback(String title) {
        return new SummaryAndTags("（自动摘要生成失败，可稍后重新生成）", List.of("未分类"));
    }
}
