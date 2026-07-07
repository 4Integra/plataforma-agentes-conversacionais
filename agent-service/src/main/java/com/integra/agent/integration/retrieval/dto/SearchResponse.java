package com.integra.agent.integration.retrieval.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(List<SearchResultItem> results) {

    public record SearchResultItem(
            float score,
            String text,
            Map<String, Object> metadata,
            String modelName
    ) {}
}
