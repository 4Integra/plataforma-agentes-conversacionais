package com.integra.agent.controller.dto;

import java.util.UUID;

public record ChatRequest(
        String message,
        UUID conversationId,
        String userId) {
}
