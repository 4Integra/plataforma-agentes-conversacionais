package com.integra.agent.integration.retrieval.dto;

import java.util.Map;

public record DocumentRequest(String text, Map<String, Object> metadata) {
    public DocumentRequest(String text) {
        this(text, Map.of());
    }
}
