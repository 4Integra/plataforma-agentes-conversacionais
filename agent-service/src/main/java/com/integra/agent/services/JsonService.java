package com.integra.agent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integra.agent.model.Tool;
import com.integra.agent.model.ToolParameters;
import org.springframework.stereotype.Service;

@Service
public class JsonService {
    public JsonService() {}

    private final ObjectMapper mapper = new ObjectMapper();

    public String toJson(ToolParameters schema) {
        try {
            return mapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public Tool fromJson(String jsonString) {
        try {
            return mapper.readValue(jsonString, Tool.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public String getStringFromJson(String json) {
        try {
            return mapper.readValue(json, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
