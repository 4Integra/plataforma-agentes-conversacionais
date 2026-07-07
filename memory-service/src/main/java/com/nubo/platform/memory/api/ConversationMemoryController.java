package com.nubo.platform.memory.api;

import com.nubo.platform.memory.api.dto.AppendMessageRequest;
import com.nubo.platform.memory.api.dto.ConversationResponse;
import com.nubo.platform.memory.api.dto.CreateConversationRequest;
import com.nubo.platform.memory.api.dto.MemoryContextResponse;
import com.nubo.platform.memory.api.dto.MessageHistoryResponse;
import com.nubo.platform.memory.api.dto.MessageResponse;
import com.nubo.platform.memory.application.ConversationMemoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationMemoryController {

    private final ConversationMemoryService service;

    public ConversationMemoryController(ConversationMemoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = service.createConversation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{conversationId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getConversation(@PathVariable UUID conversationId) {
        return service.getConversation(conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> appendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody AppendMessageRequest request) {
        MessageResponse response = service.appendMessage(conversationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{messageId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{conversationId}/messages")
    public MessageHistoryResponse listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "50") @Min(1) int limit,
            @RequestParam(required = false) Instant before) {
        return service.listMessages(conversationId, limit, before);
    }

    @GetMapping("/{conversationId}/memory")
    public MemoryContextResponse getMemory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return service.getRecentMemory(conversationId, limit);
    }

    @DeleteMapping("/{conversationId}/memory/cache")
    public ResponseEntity<Void> invalidateShortTermMemory(@PathVariable UUID conversationId) {
        service.invalidateShortTermMemory(conversationId);
        return ResponseEntity.noContent().build();
    }
}

