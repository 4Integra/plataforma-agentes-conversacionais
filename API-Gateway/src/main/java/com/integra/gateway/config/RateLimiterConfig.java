package com.integra.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the Bucket4j + Caffeine-backed AsyncProxyManager used by the
 * Spring Cloud Gateway MVC rate-limiter filter.
 *
 * <p>Each named bucket profile is expressed as a simple record so that
 * {@link GatewayRoutesConfig} can reference them without hard-coding magic
 * numbers.</p>
 *
 * <p>Limits overview:
 * <ul>
 *   <li><b>agent</b>   — 30 req / 60 s  (LLM calls are expensive)</li>
 *   <li><b>llm</b>     — 20 req / 60 s  (direct proxy to LiteLLM)</li>
 *   <li><b>default</b> — 100 req / 60 s (tools, memory, retrieval)</li>
 * </ul>
 * </p>
 */
@Configuration
public class RateLimiterConfig {

    // ── Caffeine proxy manager ────────────────────────────────────────────────

    /**
     * Shared in-process cache for bucket state.
     * maximumSize controls how many distinct keys (e.g. IPs) are tracked before
     * the least-recently-used entries are evicted.
     */
    @Bean
    public AsyncProxyManager<String> caffeineProxyManager() {
        Caffeine<Object, Object> rawBuilder = Caffeine.newBuilder().maximumSize(10_000);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Caffeine<String, RemoteBucketState> builder =
                (Caffeine<String, RemoteBucketState>) (Caffeine) rawBuilder;
        return new CaffeineProxyManager<>(builder, Duration.ofMinutes(10)).asAsync();
    }

    // ── Bucket profiles ───────────────────────────────────────────────────────

    /** Returns the rate-limit profile for agent-service routes. */
    public static BucketProfile agentProfile() {
        return new BucketProfile(30, Duration.ofMinutes(1));
    }

    /** Returns the rate-limit profile for llm-gateway routes. */
    public static BucketProfile llmProfile() {
        return new BucketProfile(20, Duration.ofMinutes(1));
    }

    /** Returns the default rate-limit profile (tools, memory, retrieval). */
    public static BucketProfile defaultProfile() {
        return new BucketProfile(100, Duration.ofMinutes(1));
    }

    /**
     * Simple value object carrying capacity + refill period for a bucket.
     *
     * @param capacity max tokens in the bucket (= max burst)
     * @param period   duration after which capacity is fully regenerated
     */
    public record BucketProfile(int capacity, Duration period) {}
}
