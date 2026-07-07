package com.nubo.platform.memory.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nubo.platform.memory.api.dto.AppendMessageRequest;
import com.nubo.platform.memory.api.dto.CreateConversationRequest;
import com.nubo.platform.memory.api.dto.MemoryContextResponse;
import com.nubo.platform.memory.api.dto.MessageResponse;
import com.nubo.platform.memory.config.MemoryProperties;
import com.nubo.platform.memory.domain.ConversationEntity;
import com.nubo.platform.memory.domain.ConversationMessageEntity;
import com.nubo.platform.memory.domain.MessageRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationMemoryServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
    private FakeStorage storage;
    private FakeShortTermMemoryStore shortTermMemoryStore;
    private ConversationMemoryService service;

    @BeforeEach
    void setUp() {
        MemoryProperties properties = new MemoryProperties();
        storage = new FakeStorage();
        shortTermMemoryStore = new FakeShortTermMemoryStore();
        service = new ConversationMemoryService(
                storage,
                shortTermMemoryStore,
                new MemoryMapper(),
                properties,
                clock);
    }

    @Test
    void createsConversationWithDefaults() {
        var response = service.createConversation(new CreateConversationRequest(
                "user-1",
                null,
                "Demo",
                Map.of("tenant", "local")));

        assertThat(response.id()).isNotNull();
        assertThat(response.agentId()).isEqualTo("default-agent");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-07-06T12:00:00Z"));
        assertThat(storage.conversations).containsKey(response.id());
    }

    @Test
    void appendsMessageToPostgresAndShortTermMemory() {
        var conversation = service.createConversation(new CreateConversationRequest("user-1", "agent-1", null, null));

        MessageResponse message = service.appendMessage(conversation.id(), new AppendMessageRequest(
                MessageRole.USER,
                "Hello",
                "llama3.1",
                null,
                "request-1",
                10,
                null,
                Map.of("channel", "test")));

        assertThat(storage.messages).hasSize(1);
        assertThat(shortTermMemoryStore.readRecent(conversation.id(), 10)).containsExactly(message);
    }

    @Test
    void returnsRedisMemoryWhenAvailable() {
        var conversation = service.createConversation(new CreateConversationRequest("user-1", "agent-1", null, null));
        MessageResponse message = service.appendMessage(conversation.id(), new AppendMessageRequest(
                MessageRole.ASSISTANT,
                "Cached response",
                null,
                null,
                null,
                null,
                null,
                null));

        MemoryContextResponse response = service.getRecentMemory(conversation.id(), 20);

        assertThat(response.source()).isEqualTo("redis");
        assertThat(response.messages()).containsExactly(message);
    }

    @Test
    void fallsBackToLongTermHistoryInChronologicalOrder() {
        var conversation = service.createConversation(new CreateConversationRequest("user-1", "agent-1", null, null));
        service.appendMessage(conversation.id(), new AppendMessageRequest(MessageRole.USER, "First", null, null, null, null, null, null));
        service.appendMessage(conversation.id(), new AppendMessageRequest(MessageRole.ASSISTANT, "Second", null, null, null, null, null, null));
        shortTermMemoryStore.invalidate(conversation.id());

        MemoryContextResponse response = service.getRecentMemory(conversation.id(), 20);

        assertThat(response.source()).isEqualTo("postgres");
        assertThat(response.messages()).extracting(MessageResponse::content).containsExactly("First", "Second");
        assertThat(shortTermMemoryStore.readRecent(conversation.id(), 20)).extracting(MessageResponse::content)
                .containsExactly("First", "Second");
    }

    private static class FakeStorage implements ConversationMemoryStorage {
        private final Map<UUID, ConversationEntity> conversations = new HashMap<>();
        private final List<ConversationMessageEntity> messages = new ArrayList<>();

        @Override
        public ConversationEntity saveConversation(ConversationEntity conversation) {
            conversations.put(conversation.getId(), conversation);
            return conversation;
        }

        @Override
        public Optional<ConversationEntity> findConversation(UUID conversationId) {
            return Optional.ofNullable(conversations.get(conversationId));
        }

        @Override
        public boolean conversationExists(UUID conversationId) {
            return conversations.containsKey(conversationId);
        }

        @Override
        public ConversationMessageEntity saveMessage(ConversationMessageEntity message) {
            messages.add(message);
            return message;
        }

        @Override
        public List<ConversationMessageEntity> findMessages(UUID conversationId, Instant before, int limit) {
            return messages.stream()
                    .filter(message -> message.getConversation().getId().equals(conversationId))
                    .filter(message -> before == null || message.getCreatedAt().isBefore(before))
                    .sorted(this::newestFirst)
                    .limit(limit)
                    .toList();
        }

        private int newestFirst(ConversationMessageEntity left, ConversationMessageEntity right) {
            int createdAtComparison = right.getCreatedAt().compareTo(left.getCreatedAt());
            if (createdAtComparison != 0) {
                return createdAtComparison;
            }
            return Integer.compare(messages.indexOf(right), messages.indexOf(left));
        }
    }

    private static class FakeShortTermMemoryStore implements ShortTermMemoryStore {
        private final Map<UUID, List<MessageResponse>> messagesByConversation = new HashMap<>();

        @Override
        public void append(UUID conversationId, MessageResponse message) {
            messagesByConversation.computeIfAbsent(conversationId, ignored -> new ArrayList<>()).add(message);
        }

        @Override
        public List<MessageResponse> readRecent(UUID conversationId, int limit) {
            List<MessageResponse> messages = messagesByConversation.getOrDefault(conversationId, List.of());
            int fromIndex = Math.max(0, messages.size() - limit);
            return List.copyOf(messages.subList(fromIndex, messages.size()));
        }

        @Override
        public void replaceRecent(UUID conversationId, List<MessageResponse> messages) {
            messagesByConversation.put(conversationId, new ArrayList<>(messages));
        }

        @Override
        public void invalidate(UUID conversationId) {
            messagesByConversation.remove(conversationId);
        }
    }
}
