package com.nubo.platform.memory.application;

import com.nubo.platform.memory.api.dto.AppendMessageRequest;
import com.nubo.platform.memory.api.dto.ConversationResponse;
import com.nubo.platform.memory.api.dto.CreateConversationRequest;
import com.nubo.platform.memory.api.dto.MemoryContextResponse;
import com.nubo.platform.memory.api.dto.MessageHistoryResponse;
import com.nubo.platform.memory.api.dto.MessageResponse;
import com.nubo.platform.memory.config.MemoryProperties;
import com.nubo.platform.memory.domain.ConversationEntity;
import com.nubo.platform.memory.domain.ConversationMessageEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class ConversationMemoryService {

    private static final String DEFAULT_AGENT_ID = "default-agent";

    private final ConversationMemoryStorage storage;
    private final ShortTermMemoryStore shortTermMemoryStore;
    private final MemoryMapper mapper;
    private final MemoryProperties properties;
    private final Clock clock;

    public ConversationMemoryService(
            ConversationMemoryStorage storage,
            ShortTermMemoryStore shortTermMemoryStore,
            MemoryMapper mapper,
            MemoryProperties properties,
            Clock clock) {
        this.storage = storage;
        this.shortTermMemoryStore = shortTermMemoryStore;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Instant now = clock.instant();
        ConversationEntity conversation = new ConversationEntity();
        conversation.setId(UUID.randomUUID());
        conversation.setUserId(request.userId().trim());
        conversation.setAgentId(textOrDefault(request.agentId(), DEFAULT_AGENT_ID));
        conversation.setTitle(normalizeOptionalText(request.title()));
        conversation.setMetadata(request.metadata());
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        return mapper.toConversationResponse(storage.saveConversation(conversation));
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId) {
        return storage.findConversation(conversationId)
                .map(mapper::toConversationResponse)
                .orElseThrow(() -> ResourceNotFoundException.conversation(conversationId));
    }

    @Transactional
    public MessageResponse appendMessage(UUID conversationId, AppendMessageRequest request) {
        ConversationEntity conversation = storage.findConversation(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.conversation(conversationId));

        Instant now = clock.instant();
        ConversationMessageEntity message = new ConversationMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRole(request.role());
        message.setContent(request.content());
        message.setModel(normalizeOptionalText(request.model()));
        message.setToolName(normalizeOptionalText(request.toolName()));
        message.setCorrelationId(normalizeOptionalText(request.correlationId()));
        message.setPromptTokens(request.promptTokens());
        message.setCompletionTokens(request.completionTokens());
        message.setMetadata(request.metadata());
        message.setCreatedAt(now);

        conversation.setUpdatedAt(now);
        storage.saveConversation(conversation);

        MessageResponse response = mapper.toMessageResponse(storage.saveMessage(message));
        afterCommit(() -> shortTermMemoryStore.append(conversationId, response));
        return response;
    }

    @Transactional(readOnly = true)
    public MessageHistoryResponse listMessages(UUID conversationId, int requestedLimit, Instant before) {
        requireConversation(conversationId);

        int limit = normalizeLimit(requestedLimit);
        List<ConversationMessageEntity> rows = storage.findMessages(conversationId, before, limit + 1);
        boolean hasMore = rows.size() > limit;
        List<ConversationMessageEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<MessageResponse> messages = page.stream()
                .map(mapper::toMessageResponse)
                .toList();
        Instant nextBefore = hasMore && !messages.isEmpty()
                ? messages.get(messages.size() - 1).createdAt()
                : null;

        return new MessageHistoryResponse(conversationId, messages, limit, before, nextBefore, hasMore);
    }

    @Transactional(readOnly = true)
    public MemoryContextResponse getRecentMemory(UUID conversationId, int requestedLimit) {
        requireConversation(conversationId);

        int limit = normalizeLimit(requestedLimit);
        List<MessageResponse> cachedMessages = shortTermMemoryStore.readRecent(conversationId, limit);
        if (!cachedMessages.isEmpty()) {
            return new MemoryContextResponse(conversationId, cachedMessages, "redis", limit, clock.instant());
        }

        List<ConversationMessageEntity> rows = new ArrayList<>(storage.findMessages(conversationId, null, limit));
        Collections.reverse(rows);
        List<MessageResponse> messages = rows.stream()
                .map(mapper::toMessageResponse)
                .toList();

        afterCommit(() -> shortTermMemoryStore.replaceRecent(conversationId, messages));
        return new MemoryContextResponse(conversationId, messages, "postgres", limit, clock.instant());
    }

    @Transactional(readOnly = true)
    public void invalidateShortTermMemory(UUID conversationId) {
        requireConversation(conversationId);
        shortTermMemoryStore.invalidate(conversationId);
    }

    private void requireConversation(UUID conversationId) {
        if (!storage.conversationExists(conversationId)) {
            throw ResourceNotFoundException.conversation(conversationId);
        }
    }

    private int normalizeLimit(int requestedLimit) {
        if (requestedLimit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        return Math.min(requestedLimit, properties.getMaxPageSize());
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
