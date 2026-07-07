package com.nubo.platform.memory.application;

import com.nubo.platform.memory.api.dto.ConversationResponse;
import com.nubo.platform.memory.api.dto.MessageResponse;
import com.nubo.platform.memory.domain.ConversationEntity;
import com.nubo.platform.memory.domain.ConversationMessageEntity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemoryMapper {

    public ConversationResponse toConversationResponse(ConversationEntity entity) {
        return new ConversationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getAgentId(),
                entity.getTitle(),
                immutableMetadata(entity.getMetadata()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public MessageResponse toMessageResponse(ConversationMessageEntity entity) {
        return new MessageResponse(
                entity.getId(),
                entity.getConversation().getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getModel(),
                entity.getToolName(),
                entity.getCorrelationId(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                immutableMetadata(entity.getMetadata()),
                entity.getCreatedAt());
    }

    private Map<String, Object> immutableMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

