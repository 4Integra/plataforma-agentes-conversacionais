package com.nubo.platform.memory.infrastructure.postgres;

import com.nubo.platform.memory.application.ConversationMemoryStorage;
import com.nubo.platform.memory.domain.ConversationEntity;
import com.nubo.platform.memory.domain.ConversationMessageEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class ConversationMemoryJpaStorage implements ConversationMemoryStorage {

    private final ConversationJpaRepository conversationRepository;
    private final ConversationMessageJpaRepository messageRepository;

    ConversationMemoryJpaStorage(
            ConversationJpaRepository conversationRepository,
            ConversationMessageJpaRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public ConversationEntity saveConversation(ConversationEntity conversation) {
        return conversationRepository.save(conversation);
    }

    @Override
    public Optional<ConversationEntity> findConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId);
    }

    @Override
    public boolean conversationExists(UUID conversationId) {
        return conversationRepository.existsById(conversationId);
    }

    @Override
    public ConversationMessageEntity saveMessage(ConversationMessageEntity message) {
        return messageRepository.save(message);
    }

    @Override
    public List<ConversationMessageEntity> findMessages(UUID conversationId, Instant before, int limit) {
        return messageRepository.findMessages(conversationId, before, PageRequest.of(0, limit));
    }
}

