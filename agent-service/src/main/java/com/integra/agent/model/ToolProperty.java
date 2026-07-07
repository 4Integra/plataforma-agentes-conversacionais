package com.integra.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ToolProperty {

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    // Construtores
    public ToolProperty() {}

    public ToolProperty(String type) {
        this.type = type;
    }

    public ToolProperty(String type, String description) {
        this.type = type;
        this.description = description;
    }

    // Getters e Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolProperty property = (ToolProperty) o;
        return Objects.equals(type, property.type) &&
                Objects.equals(description, property.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description);
    }

    @Override
    public String toString() {
        return "Property{" +
                "type='" + type + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}