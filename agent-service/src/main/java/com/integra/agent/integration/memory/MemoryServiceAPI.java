package com.integra.agent.integration.memory;

import com.integra.agent.integration.memory.dto.AppendMessageRequest;
import com.integra.agent.integration.memory.dto.ConversationResponse;
import com.integra.agent.integration.memory.dto.CreateConversationRequest;
import com.integra.agent.integration.memory.dto.MessageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "memoryService", url = "${memory.base.url}")
public interface MemoryServiceAPI {

    @PostMapping("/api/v1/conversations")
    ConversationResponse createConversation(@RequestBody CreateConversationRequest request);

    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    MessageResponse appendMessage(
            @PathVariable UUID conversationId,
            @RequestBody AppendMessageRequest request);
}
