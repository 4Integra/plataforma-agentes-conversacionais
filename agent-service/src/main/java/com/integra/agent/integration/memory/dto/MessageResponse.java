package com.integra.agent.integration.memory.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        String model,
        String toolName,
        String correlationId,
        Integer promptTokens,
        Integer completionTokens,
        Map<String, Object> metadata,
        Instant createdAt) {
}
