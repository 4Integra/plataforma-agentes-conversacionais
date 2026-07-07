package com.integra.agent.integration.tools;

import com.integra.agent.model.Tool;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "toolService", url = "${tool.base.url}")
public interface ToolRegistryAPI {
    @GetMapping
    List<Tool> getTools();
}
