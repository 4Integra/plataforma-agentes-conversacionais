package com.integra.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Handles circuit breaker fallback responses for each downstream service.
 *
 * <p>When a circuit breaker trips (OPEN state) or a timeout occurs, the gateway
 * forwards the request to one of these endpoints instead of returning a raw
 * 5xx error, giving the caller a structured, human-readable response.</p>
 *
 * <p>Fallback URL mapping (configured in {@link com.integra.gateway.config.GatewayRoutesConfig}):
 * <ul>
 *   <li>{@code /fallback/agent}      ← agent-service circuit breaker</li>
 *   <li>{@code /fallback/tools}      ← tool-registry circuit breaker</li>
 *   <li>{@code /fallback/memory}     ← memory-service circuit breaker</li>
 *   <li>{@code /fallback/retrieval}  ← retrieval-service circuit breaker</li>
 *   <li>{@code /fallback/llm}        ← llm-gateway circuit breaker</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/agent")
    public ResponseEntity<Map<String, Object>> agentFallback(HttpServletRequest request) {
        return fallback("agent-service", "O serviço de agente está temporariamente indisponível. Tente novamente em instantes.", request);
    }

    @RequestMapping("/tools")
    public ResponseEntity<Map<String, Object>> toolsFallback(HttpServletRequest request) {
        return fallback("tool-registry", "O registro de ferramentas está temporariamente indisponível. Tente novamente em instantes.", request);
    }

    @RequestMapping("/memory")
    public ResponseEntity<Map<String, Object>> memoryFallback(HttpServletRequest request) {
        return fallback("memory-service", "O serviço de memória está temporariamente indisponível. Tente novamente em instantes.", request);
    }

    @RequestMapping("/retrieval")
    public ResponseEntity<Map<String, Object>> retrievalFallback(HttpServletRequest request) {
        return fallback("retrieval-service", "O serviço de recuperação (RAG) está temporariamente indisponível. Tente novamente em instantes.", request);
    }

    @RequestMapping("/llm")
    public ResponseEntity<Map<String, Object>> llmFallback(HttpServletRequest request) {
        return fallback("llm-gateway", "O gateway de LLM está temporariamente indisponível. Verifique se o Ollama/LiteLLM está em execução.", request);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> fallback(String service, String message,
                                                          HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        log.warn("Circuit breaker fallback triggered for service='{}' requestId='{}'",
                service, requestId);

        Map<String, Object> body = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "service", service,
                "message", message,
                "requestId", requestId != null ? requestId : "unknown",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
}
