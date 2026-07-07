package com.nubo.platform.memory.application;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    private ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException conversation(UUID conversationId) {
        return new ResourceNotFoundException("Conversation not found: " + conversationId);
    }
}

