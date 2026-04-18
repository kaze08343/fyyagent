package org.example.yuaiagent.chatmemory.mangodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);
}