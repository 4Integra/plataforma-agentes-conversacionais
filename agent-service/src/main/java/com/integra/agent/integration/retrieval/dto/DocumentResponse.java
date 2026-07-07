package com.integra.agent.integration.retrieval.dto;

public record DocumentResponse(
        String status,
        String id,
        String queueName,
        String collectionName,
        String modelName
) {}
