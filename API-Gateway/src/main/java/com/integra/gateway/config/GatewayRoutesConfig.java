package com.integra.gateway.config;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * Defines all gateway routes using the Spring Cloud Gateway Server MVC Java DSL.
 *
 * <p>Route table:
 * <pre>
 *  Gateway path              →  Downstream URL
 *  ─────────────────────────────────────────────────────────────────────────
 *  /api/agent/**             →  AGENT_SERVICE_URL    (default :8080)
 *  /api/tools/**             →  TOOL_REGISTRY_URL    (default :8081)
 *  /api/memory/**            →  MEMORY_SERVICE_URL   (default :8083)
 *  /api/retrieval/**         →  RETRIEVAL_SERVICE_URL (default :8000)
 *  /api/llm/**               →  LLM_GATEWAY_URL      (default :4000)
 * </pre>
 * </p>
 *
 * <p>Every route applies, in order:
 * <ol>
 *   <li>Path rewrite — strips the gateway prefix before forwarding</li>
 *   <li>X-Request-Id — injects a UUID header for distributed tracing</li>
 *   <li>Rate limiting — per-IP token bucket via Bucket4j + Caffeine</li>
 *   <li>Circuit breaker — Resilience4j wraps the downstream call; on trip,
 *       forwards to the corresponding {@code /fallback/*} endpoint</li>
 * </ol>
 * </p>
 */
@Configuration
public class GatewayRoutesConfig {

    private final AsyncProxyManager<String> proxyManager;

    @Value("${gateway.services.agent}")
    private String agentServiceUrl;

    @Value("${gateway.services.tools}")
    private String toolRegistryUrl;

    @Value("${gateway.services.memory}")
    private String memoryServiceUrl;

    @Value("${gateway.services.retrieval}")
    private String retrievalServiceUrl;

    @Value("${gateway.services.llm}")
    private String llmGatewayUrl;

    public GatewayRoutesConfig(AsyncProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    // ── Agent Service ──────────────────────────────────────────────────────────
    // /api/agent/{rest}  →  /{rest}
    @Bean
    public RouterFunction<ServerResponse> agentRoutes() {
        RateLimiterConfig.BucketProfile rl = RateLimiterConfig.agentProfile();
        return route("agent-service")
                .route(path("/api/agent/**"), http())
                .before(uri(URI.create(agentServiceUrl)))
                .before(rewritePath("/api/agent/(?<segment>.*)", "/${segment}"))
                .before(requestIdHeader())
                .filter(rateLimit(c -> c
                        .setCapacity(rl.capacity())
                        .setPeriod(rl.period())
                        .setKeyResolver(req -> clientIp(req.servletRequest()))))
                .filter(circuitBreaker("agent-cb", URI.create("forward:/fallback/agent")))
                .build();
    }

    // ── Tool Registry ──────────────────────────────────────────────────────────
    // /api/tools/{rest}  →  /tools/{rest}
    @Bean
    public RouterFunction<ServerResponse> toolRegistryRoutes() {
        RateLimiterConfig.BucketProfile rl = RateLimiterConfig.defaultProfile();
        return route("tool-registry")
                .route(path("/api/tools/**"), http())
                .before(uri(URI.create(toolRegistryUrl)))
                .before(rewritePath("/api/tools/(?<segment>.*)", "/tools/${segment}"))
                .before(requestIdHeader())
                .filter(rateLimit(c -> c
                        .setCapacity(rl.capacity())
                        .setPeriod(rl.period())
                        .setKeyResolver(req -> clientIp(req.servletRequest()))))
                .filter(circuitBreaker("default-cb", URI.create("forward:/fallback/tools")))
                .build();
    }

    // ── Memory Service ─────────────────────────────────────────────────────────
    // /api/memory/{rest}  →  /api/v1/conversations/{rest}
    @Bean
    public RouterFunction<ServerResponse> memoryServiceRoutes() {
        RateLimiterConfig.BucketProfile rl = RateLimiterConfig.defaultProfile();
        return route("memory-service")
                .route(path("/api/memory/**"), http())
                .before(uri(URI.create(memoryServiceUrl)))
                .before(rewritePath("/api/memory/(?<segment>.*)", "/api/v1/conversations/${segment}"))
                .before(requestIdHeader())
                .filter(rateLimit(c -> c
                        .setCapacity(rl.capacity())
                        .setPeriod(rl.period())
                        .setKeyResolver(req -> clientIp(req.servletRequest()))))
                .filter(circuitBreaker("default-cb", URI.create("forward:/fallback/memory")))
                .build();
    }

    // ── Retrieval / RAG Service ────────────────────────────────────────────────
    // /api/retrieval/{rest}  →  /{rest}
    @Bean
    public RouterFunction<ServerResponse> retrievalServiceRoutes() {
        RateLimiterConfig.BucketProfile rl = RateLimiterConfig.defaultProfile();
        return route("retrieval-service")
                .route(path("/api/retrieval/**"), http())
                .before(uri(URI.create(retrievalServiceUrl)))
                .before(rewritePath("/api/retrieval/(?<segment>.*)", "/${segment}"))
                .before(requestIdHeader())
                .filter(rateLimit(c -> c
                        .setCapacity(rl.capacity())
                        .setPeriod(rl.period())
                        .setKeyResolver(req -> clientIp(req.servletRequest()))))
                .filter(circuitBreaker("default-cb", URI.create("forward:/fallback/retrieval")))
                .build();
    }

    // ── LLM Gateway (LiteLLM proxy) ────────────────────────────────────────────
    // /api/llm/{rest}  →  /{rest}
    @Bean
    public RouterFunction<ServerResponse> llmGatewayRoutes() {
        RateLimiterConfig.BucketProfile rl = RateLimiterConfig.llmProfile();
        return route("llm-gateway")
                .route(path("/api/llm/**"), http())
                .before(uri(URI.create(llmGatewayUrl)))
                .before(rewritePath("/api/llm/(?<segment>.*)", "/${segment}"))
                .before(requestIdHeader())
                .filter(rateLimit(c -> c
                        .setCapacity(rl.capacity())
                        .setPeriod(rl.period())
                        .setKeyResolver(req -> clientIp(req.servletRequest()))))
                .filter(circuitBreaker("llm-cb", URI.create("forward:/fallback/llm")))
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Injects a unique {@code X-Request-Id} (UUID) into every forwarded request
     * to enable distributed tracing across downstream services.
     */
    private static java.util.function.Function<
            org.springframework.web.servlet.function.ServerRequest,
            org.springframework.web.servlet.function.ServerRequest> requestIdHeader() {
        return addRequestHeader("X-Request-Id",
                java.util.UUID.randomUUID().toString());
    }

    /**
     * Extracts the real client IP, honouring {@code X-Forwarded-For} when the
     * request arrived through a load balancer or reverse proxy.
     */
    private static String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
