package com.nubo.platform.memory.application;

import com.nubo.platform.memory.domain.ConversationEntity;
import com.nubo.platform.memory.domain.ConversationMessageEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemoryStorage {

    ConversationEntity saveConversation(ConversationEntity conversation);

    Optional<ConversationEntity> findConversation(UUID conversationId);

    boolean conversationExists(UUID conversationId);

    ConversationMessageEntity saveMessage(ConversationMessageEntity message);

    List<ConversationMessageEntity> findMessages(UUID conversationId, Instant before, int limit);
}

