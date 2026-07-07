package com.nubo.platform.memory.api.dto;

import com.nubo.platform.memory.domain.MessageRole;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageRole role,
        String content,
        String model,
        String toolName,
        String correlationId,
        Integer promptTokens,
        Integer completionTokens,
        Map<String, Object> metadata,
        Instant createdAt) {
}

