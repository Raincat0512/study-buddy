package com.raincat.studybuddy.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findAllByOrderByCreatedAtDesc();

    /** tags 以逗号分隔存储，这里用包含匹配做标签检索 */
    List<DocumentEntity> findByTagsContainingIgnoreCase(String tagFragment);
}
