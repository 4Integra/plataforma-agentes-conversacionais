package com.integra.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures Resilience4j circuit breakers for each downstream service.
 *
 * <p>Circuit breaker profiles:
 * <pre>
 *  Name               Slow-call threshold   Failure threshold   Wait (open→half-open)   Timeout
 *  ─────────────────────────────────────────────────────────────────────────────────────────────
 *  agent-cb           60 % > 2 s            50 % in 10 calls    15 s                    30 s
 *  llm-cb             60 % > 5 s            50 % in 10 calls    30 s                    60 s
 *  default-cb         60 % > 2 s            50 % in 10 calls    10 s                    10 s
 * </pre>
 * LLM calls are intentionally more lenient (longer timeouts, longer wait) because
 * model inference can be slow under load without being truly broken.
 * </p>
 *
 * <p>States:
 * <ul>
 *   <li><b>CLOSED</b>  — normal operation, requests pass through</li>
 *   <li><b>OPEN</b>    — downstream is unhealthy, requests fail fast → fallback</li>
 *   <li><b>HALF-OPEN</b> — probe requests sent; closes if they succeed, reopens if not</li>
 * </ul>
 * </p>
 */
@Configuration
public class CircuitBreakerConfig {

    // ── Default (tools, memory, retrieval) ───────────────────────────────────

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(timeLimiter(Duration.ofSeconds(10)))
                .circuitBreakerConfig(defaultCbConfig())
                .build());
    }

    // ── Agent Service ─────────────────────────────────────────────────────────

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> agentCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder
                        .timeLimiterConfig(timeLimiter(Duration.ofSeconds(30)))
                        .circuitBreakerConfig(agentCbConfig()),
                "agent-cb");
    }

    // ── LLM Gateway ───────────────────────────────────────────────────────────

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> llmCircuitBreakerCustomizer() {
        return factory -> factory.configure(
                builder -> builder
                        .timeLimiterConfig(timeLimiter(Duration.ofSeconds(60)))
                        .circuitBreakerConfig(llmCbConfig()),
                "llm-cb");
    }

    // ── Shared registry exposure (for /actuator/circuitbreakers) ─────────────

    /**
     * Exposes the Resilience4j registry so that actuator can report circuit
     * breaker states at {@code GET /actuator/circuitbreakers}.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static TimeLimiterConfig timeLimiter(Duration timeout) {
        return TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .build();
    }

    /** Default profile: short timeout, moderate failure sensitivity. */
    private static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultCbConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)                     // open if ≥ 50 % of last 10 calls fail
                .slowCallRateThreshold(60)                    // also open if ≥ 60 % are slow
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();
    }

    /** Agent profile: tolerates slightly longer calls (LLM round-trips). */
    private static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig agentCbConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .slowCallRateThreshold(60)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();
    }

    /** LLM profile: long timeouts, more patience before declaring the circuit open. */
    private static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig llmCbConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .slowCallRateThreshold(60)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();
    }
}
