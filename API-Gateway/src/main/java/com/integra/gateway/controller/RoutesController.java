package com.integra.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Diagnostic endpoint that lists all configured gateway routes and their
 * downstream targets. Available at GET /actuator/gateway-routes.
 *
 * This exists because spring-cloud-gateway-server-webmvc does not expose
 * the /actuator/gateway endpoint that the reactive variant provides.
 */
@RestController
@RequestMapping("/actuator/gateway-routes")
public class RoutesController {

    @Value("${gateway.services.agent}")
    private String agentUrl;

    @Value("${gateway.services.tools}")
    private String toolsUrl;

    @Value("${gateway.services.memory}")
    private String memoryUrl;

    @Value("${gateway.services.retrieval}")
    private String retrievalUrl;

    @Value("${gateway.services.llm}")
    private String llmUrl;

    @GetMapping
    public List<Map<String, String>> routes() {
        return List.of(
            route("agent-service",    "/api/agent/**",     agentUrl,     "/chat, /chat-with-tool",  "30 req/min"),
            route("tool-registry",    "/api/tools/**",     toolsUrl,     "/tools",                  "100 req/min"),
            route("memory-service",   "/api/memory/**",    memoryUrl,    "/api/v1/conversations/**","100 req/min"),
            route("retrieval-service","/api/retrieval/**", retrievalUrl, "/**",                     "100 req/min"),
            route("llm-gateway",      "/api/llm/**",       llmUrl,       "/**",                     "20 req/min")
        );
    }

    private Map<String, String> route(String id, String path, String upstream,
                                       String upstreamPath, String rateLimit) {
        return Map.of(
            "id",           id,
            "path",         path,
            "upstream",     upstream,
            "upstreamPath", upstreamPath,
            "rateLimit",    rateLimit
        );
    }
}
