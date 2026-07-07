package com.nubo.platform.memory.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    @Min(1)
    private int maxPageSize = 200;

    @Valid
    private final ShortTerm shortTerm = new ShortTerm();

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public ShortTerm getShortTerm() {
        return shortTerm;
    }

    public static class ShortTerm {
        private boolean enabled = true;

        @Min(1)
        private int maxMessages = 50;

        @NotNull
        private Duration ttl = Duration.ofHours(12);

        @NotBlank
        private String keyPrefix = "memory:conversation:";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
