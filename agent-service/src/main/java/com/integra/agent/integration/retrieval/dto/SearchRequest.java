package com.integra.agent.integration.retrieval.dto;

public record SearchRequest(String query, int limit) {
    public SearchRequest(String query) {
        this(query, 5);
    }
}
