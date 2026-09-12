package com.raincat.studybuddy.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceType sourceType;

    /** 网页 URL 或原始文件名 */
    @Column(length = 1024)
    private String sourceRef;

    /** 抽取后的全文，向量库中的每个分块都通过 metadata.docId 指回这里 */
    @Column(columnDefinition = "text")
    private String content;

    /** LLM 生成的摘要 */
    @Column(columnDefinition = "text")
    private String summary;

    /** LLM 生成的标签，逗号分隔 */
    @Column(columnDefinition = "text")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(nullable = false)
    private int charCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public DocumentEntity() {
    }

    public DocumentEntity(String title, SourceType sourceType, String sourceRef, String content) {
        this.title = title;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.content = content;
        this.charCount = content == null ? 0 : content.length();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public int getCharCount() {
        return charCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
