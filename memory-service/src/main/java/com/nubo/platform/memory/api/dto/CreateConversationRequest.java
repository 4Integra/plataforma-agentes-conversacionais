package com.nubo.platform.memory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateConversationRequest(
        @NotBlank @Size(max = 128) String userId,
        @Size(max = 128) String agentId,
        @Size(max = 200) String title,
        Map<String, Object> metadata) {
}

