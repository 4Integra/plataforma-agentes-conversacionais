package com.nubo.platform.memory.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.platform.memory.api.dto.MessageResponse;
import com.nubo.platform.memory.application.ShortTermMemoryStore;
import com.nubo.platform.memory.config.MemoryProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisShortTermMemoryStore implements ShortTermMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisShortTermMemoryStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MemoryProperties properties;

    RedisShortTermMemoryStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MemoryProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void append(UUID conversationId, MessageResponse message) {
        if (!properties.getShortTerm().isEnabled()) {
            return;
        }

        try {
            String key = key(conversationId);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -properties.getShortTerm().getMaxMessages(), -1);
            redisTemplate.expire(key, properties.getShortTerm().getTtl());
        } catch (JsonProcessingException exception) {
            log.warn("Could not serialize message {} for short-term memory", message.id(), exception);
        } catch (RuntimeException exception) {
            log.warn("Redis short-term memory append failed for conversation {}", conversationId, exception);
        }
    }

    @Override
    public List<MessageResponse> readRecent(UUID conversationId, int limit) {
        if (!properties.getShortTerm().isEnabled()) {
            return List.of();
        }

        try {
            List<String> values = redisTemplate.opsForList().range(key(conversationId), -limit, -1);
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            List<MessageResponse> messages = new ArrayList<>(values.size());
            for (String value : values) {
                try {
                    messages.add(objectMapper.readValue(value, MessageResponse.class));
                } catch (JsonProcessingException exception) {
                    log.warn("Skipping corrupted short-term memory entry for conversation {}", conversationId, exception);
                }
            }
            return messages;
        } catch (RuntimeException exception) {
            log.warn("Redis short-term memory read failed for conversation {}", conversationId, exception);
            return List.of();
        }
    }

    @Override
    public void replaceRecent(UUID conversationId, List<MessageResponse> messages) {
        if (!properties.getShortTerm().isEnabled()) {
            return;
        }

        try {
            String key = key(conversationId);
            redisTemplate.delete(key);
            if (messages.isEmpty()) {
                return;
            }

            List<String> values = new ArrayList<>(messages.size());
            for (MessageResponse message : messages) {
                values.add(objectMapper.writeValueAsString(message));
            }
            redisTemplate.opsForList().rightPushAll(key, values);
            redisTemplate.opsForList().trim(key, -properties.getShortTerm().getMaxMessages(), -1);
            redisTemplate.expire(key, properties.getShortTerm().getTtl());
        } catch (JsonProcessingException exception) {
            log.warn("Could not serialize messages for short-term memory replacement", exception);
        } catch (RuntimeException exception) {
            log.warn("Redis short-term memory replacement failed for conversation {}", conversationId, exception);
        }
    }

    @Override
    public void invalidate(UUID conversationId) {
        if (!properties.getShortTerm().isEnabled()) {
            return;
        }

        try {
            redisTemplate.delete(key(conversationId));
        } catch (RuntimeException exception) {
            log.warn("Redis short-term memory invalidation failed for conversation {}", conversationId, exception);
        }
    }

    private String key(UUID conversationId) {
        return properties.getShortTerm().getKeyPrefix() + conversationId + ":recent";
    }
}

