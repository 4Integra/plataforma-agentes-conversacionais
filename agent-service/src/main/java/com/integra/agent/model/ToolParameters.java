package com.integra.agent.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


public class ToolParameters {

    @JsonProperty("properties")
    private Map<String, ToolProperty> properties;

    @JsonProperty("required")
    private List<String> required;

    @JsonProperty("type")
    private String type;

    public ToolParameters() {}

    public ToolParameters(Map<String, ToolProperty> properties, List<String> required) {
        this.properties = properties;
        this.required = required;
    }

    public Map<String, ToolProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, ToolProperty> properties) {
        this.properties = properties;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolParameters that = (ToolParameters) o;
        return Objects.equals(properties, that.properties) &&
                Objects.equals(required, that.required);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, required);
    }

    @Override
    public String toString() {
        return "InputSchema{" +
                "properties=" + properties +
                ", required=" + required +
                '}';
    }
}