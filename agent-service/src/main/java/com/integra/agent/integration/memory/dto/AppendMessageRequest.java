package com.integra.agent.integration.memory.dto;

import java.util.Map;

public record AppendMessageRequest(
        String role,
        String content,
        String model,
        String toolName,
        String correlationId,
        Integer promptTokens,
        Integer completionTokens,
        Map<String, Object> metadata) {
}
