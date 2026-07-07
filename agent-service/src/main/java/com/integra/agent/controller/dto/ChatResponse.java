package com.integra.agent.controller.dto;

import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        String reply) {
}
