package com.integra.agent.services.memory;

import com.integra.agent.integration.memory.MemoryServiceAPI;
import com.integra.agent.integration.memory.dto.AppendMessageRequest;
import com.integra.agent.integration.memory.dto.ConversationResponse;
import com.integra.agent.integration.memory.dto.CreateConversationRequest;
import com.integra.agent.integration.memory.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MemoryServiceAPI memoryServiceAPI;

    public MemoryService(MemoryServiceAPI memoryServiceAPI) {
        this.memoryServiceAPI = memoryServiceAPI;
    }

    public ConversationResponse createConversation(String userId, String agentId) {
        CreateConversationRequest request = new CreateConversationRequest(userId, agentId, null, null);
        ConversationResponse response = memoryServiceAPI.createConversation(request);
        log.info("Conversation created: conversationId={}, userId={}, agentId={}", response.id(), userId, agentId);
        return response;
    }

    public MessageResponse appendUserMessage(UUID conversationId, String content) {
        AppendMessageRequest request = new AppendMessageRequest(
                "USER", content, null, null, null, null, null, null);
        MessageResponse response = memoryServiceAPI.appendMessage(conversationId, request);
        log.debug("User message recorded: conversationId={}, messageId={}", conversationId, response.id());
        return response;
    }

    public MessageResponse appendAssistantMessage(UUID conversationId, String content, String model) {
        AppendMessageRequest request = new AppendMessageRequest(
                "ASSISTANT", content, model, null, null, null, null, null);
        MessageResponse response = memoryServiceAPI.appendMessage(conversationId, request);
        log.debug("Assistant message recorded: conversationId={}, messageId={}", conversationId, response.id());
        return response;
    }
}
