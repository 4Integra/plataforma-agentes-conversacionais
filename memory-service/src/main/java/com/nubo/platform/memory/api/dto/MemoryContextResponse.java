package com.nubo.platform.memory.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryContextResponse(
        UUID conversationId,
        List<MessageResponse> messages,
        String source,
        int limit,
        Instant generatedAt) {
}

