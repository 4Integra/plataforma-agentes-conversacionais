package com.integra.agent.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Tool {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private ToolParameters inputSchema;

    public Tool() {}

    public Tool(String name, String description, ToolParameters inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ToolParameters getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(ToolParameters inputSchema) {
        this.inputSchema = inputSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tool tool = (Tool) o;
        return Objects.equals(name, tool.name) &&
                Objects.equals(description, tool.description) &&
                Objects.equals(inputSchema, tool.inputSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, inputSchema);
    }

    @Override
    public String toString() {
        return "Tool{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema=" + inputSchema +
                '}';
    }
}