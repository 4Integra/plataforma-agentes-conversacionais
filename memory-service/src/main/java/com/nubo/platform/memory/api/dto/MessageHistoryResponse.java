package com.nubo.platform.memory.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageHistoryResponse(
        UUID conversationId,
        List<MessageResponse> messages,
        int limit,
        Instant before,
        Instant nextBefore,
        boolean hasMore) {
}

