package com.integra.agent.services.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;


public class ToolActionCallback implements ToolCallback {

    private final ToolDefinition definition;
    private final ToolService executor;

    public ToolActionCallback(ToolDefinition definition, ToolService executor) {
        this.definition = definition;
        this.executor = executor;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String arguments) {
        return executor.execute(
                definition.name(),
                arguments
        );
    }
}
