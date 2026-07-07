package com.integra.agent.services.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ToolService {

    private String runBcCommand(String expression) {

        try {
            String command = "bc <<< " + expression;

            Process proc = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String line = "";
            while((line = reader.readLine()) != null) {
                System.out.print(line + "\n");
            }

            proc.waitFor();

            return line;
        } catch (Exception e) {
            return "Failed to calculate results";
        }

    }

    public String execute(String toolName, String argumentsJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(argumentsJson);

            return runBcCommand(root.get("expression").asText());
        } catch (Exception e) {
            return "Failed to parse JSON";
        }
    }
}
