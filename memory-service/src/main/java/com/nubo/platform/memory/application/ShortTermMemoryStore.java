package com.nubo.platform.memory.application;

import com.nubo.platform.memory.api.dto.MessageResponse;
import java.util.List;
import java.util.UUID;

public interface ShortTermMemoryStore {

    void append(UUID conversationId, MessageResponse message);

    List<MessageResponse> readRecent(UUID conversationId, int limit);

    void replaceRecent(UUID conversationId, List<MessageResponse> messages);

    void invalidate(UUID conversationId);
}

