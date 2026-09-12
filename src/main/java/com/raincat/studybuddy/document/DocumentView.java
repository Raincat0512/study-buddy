package com.raincat.studybuddy.document;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 文档列表/详情视图，不携带全文 */
public record DocumentView(
        Long id,
        String title,
        SourceType sourceType,
        String sourceRef,
        String summary,
        List<String> tags,
        DocumentStatus status,
        int charCount,
        LocalDateTime createdAt
) {

    public static DocumentView from(DocumentEntity entity) {
        List<String> tagList = entity.getTags() == null || entity.getTags().isBlank()
                ? List.of()
                : Arrays.stream(entity.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new DocumentView(
                entity.getId(),
                entity.getTitle(),
                entity.getSourceType(),
                entity.getSourceRef(),
                entity.getSummary(),
                tagList,
                entity.getStatus(),
                entity.getCharCount(),
                entity.getCreatedAt()
        );
    }
}
