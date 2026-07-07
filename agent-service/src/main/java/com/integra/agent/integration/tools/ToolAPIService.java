package com.integra.agent.integration.tools;

import com.integra.agent.model.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ToolAPIService {
    private final ToolRegistryAPI toolFeignClient;

    public ToolAPIService(ToolRegistryAPI toolFeignClient) {
        this.toolFeignClient = toolFeignClient;
    }

    public Optional<Tool> getFirstToolOrEmpty() {
        List<Tool> tools = toolFeignClient.getTools();
        return tools.stream().findFirst();
    }
}