package com.raincat.studybuddy.chat;

import com.raincat.studybuddy.document.DocumentEntity;
import com.raincat.studybuddy.document.DocumentRepository;
import com.raincat.studybuddy.document.RetrievalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 暴露给 agent 的工具集：模型自主决定何时调用。
 * 语义检索工具让 agent 能"回头再查资料"，弥补单轮检索的遗漏。
 */
@Component
public class NoteTools {

    private final DocumentRepository documentRepository;
    private final RetrievalService retrievalService;

    public NoteTools(DocumentRepository documentRepository, RetrievalService retrievalService) {
        this.documentRepository = documentRepository;
        this.retrievalService = retrievalService;
    }

    @Tool(description = "列出用户导入的所有学习资料，返回标题、来源类型、标签和状态")
    public String listDocuments() {
        List<DocumentEntity> docs = documentRepository.findAllByOrderByCreatedAtDesc();
        if (docs.isEmpty()) {
            return "资料库为空。";
        }
        StringBuilder sb = new StringBuilder("共 " + docs.size() + " 篇资料：\n");
        for (DocumentEntity d : docs) {
            sb.append("- 《").append(d.getTitle()).append("》 [").append(d.getSourceType())
                    .append("] 状态:").append(d.getStatus())
                    .append(d.getTags() == null || d.getTags().isBlank() ? "" : " 标签:" + d.getTags())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "根据标签查找学习资料，例如标签「操作系统」或「面试」")
    public String findByTag(@ToolParam(description = "要查找的标签") String tag) {
        List<DocumentEntity> docs = documentRepository.findByTagsContainingIgnoreCase(tag);
        if (docs.isEmpty()) {
            return "没有找到标签含「" + tag + "」的资料。";
        }
        StringBuilder sb = new StringBuilder("标签含「" + tag + "」的资料：\n");
        for (DocumentEntity d : docs) {
            sb.append("- 《").append(d.getTitle()).append("》：")
                    .append(d.getSummary() == null ? "（无摘要）" : d.getSummary())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "在资料库中做语义检索，返回与查询最相关的原文片段。适合需要引用原文细节的问题")
    public String searchNotes(@ToolParam(description = "检索的主题或问题") String query) {
        List<RetrievalService.RetrievedChunk> chunks = retrievalService.retrieve(query);
        if (chunks.isEmpty()) {
            return "资料库中没有检索到与「" + query + "」相关的内容。";
        }
        return retrievalService.buildContext(chunks);
    }
}
