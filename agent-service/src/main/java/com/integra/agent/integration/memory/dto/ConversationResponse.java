package com.integra.agent.integration.memory.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String userId,
        String agentId,
        String title,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt) {
}
