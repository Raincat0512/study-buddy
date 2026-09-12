package com.raincat.studybuddy.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    void deleteBySessionId(String sessionId);
}
