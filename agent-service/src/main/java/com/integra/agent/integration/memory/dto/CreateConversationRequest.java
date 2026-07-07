package com.integra.agent.integration.memory.dto;

import java.util.Map;

public record CreateConversationRequest(
        String userId,
        String agentId,
        String title,
        Map<String, Object> metadata) {
}
