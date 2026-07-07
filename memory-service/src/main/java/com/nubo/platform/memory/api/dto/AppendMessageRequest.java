package com.nubo.platform.memory.api.dto;

import com.nubo.platform.memory.domain.MessageRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AppendMessageRequest(
        @NotNull MessageRole role,
        @NotBlank @Size(max = 200_000) String content,
        @Size(max = 128) String model,
        @Size(max = 128) String toolName,
        @Size(max = 128) String correlationId,
        @Min(0) Integer promptTokens,
        @Min(0) Integer completionTokens,
        Map<String, Object> metadata) {
}

