package com.integra.agent.controller;

import com.integra.agent.controller.dto.ChatRequest;
import com.integra.agent.controller.dto.ChatResponse;
import com.integra.agent.integration.memory.dto.ConversationResponse;
import com.integra.agent.integration.retrieval.dto.SearchResponse;
import com.integra.agent.integration.tools.ToolAPIService;
import com.integra.agent.model.Tool;
import com.integra.agent.services.JsonService;
import com.integra.agent.services.memory.MemoryService;
import com.integra.agent.services.retrieval.RetrievalService;
import com.integra.agent.services.tools.ToolActionCallback;
import com.integra.agent.services.tools.ToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class AgentController {

    private static final String DEFAULT_MODEL = "qwen2.5";
    private static final String DEFAULT_USER_ID = "anonymous";

    private final ChatClient chatClient;
    private final JsonService jsonService;
    private final ToolAPIService toolAPIService;
    private final MemoryService memoryService;
    private final RetrievalService retrievalService;

    public AgentController(JsonService jsonService,
                           ChatClient chatClient,
                           ToolAPIService toolAPIService,
                           MemoryService memoryService,
                           RetrievalService retrievalService) {
        this.jsonService = jsonService;
        this.chatClient = chatClient;
        this.toolAPIService = toolAPIService;
        this.memoryService = memoryService;
        this.retrievalService = retrievalService;
    }

    @PostMapping("/chat")
    ChatResponse chat(@RequestBody ChatRequest request) {
        UUID conversationId = resolveConversationId(request);

        memoryService.appendUserMessage(conversationId, request.message());

        String reply = chatClient.prompt()
                .user(request.message())
                .call()
                .content();

        memoryService.appendAssistantMessage(conversationId, reply, DEFAULT_MODEL);

        return new ChatResponse(conversationId, reply);
    }

    @PostMapping("/chat-with-tool")
    ChatResponse chatWithTool(@RequestBody ChatRequest request) {
        UUID conversationId = resolveConversationId(request);

        memoryService.appendUserMessage(conversationId, request.message());

        Optional<Tool> tool = toolAPIService.getFirstToolOrEmpty();
        String reply;

        if (tool.isPresent()) {
            Tool schema = tool.get();

            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name(schema.getName())
                    .description(schema.getDescription())
                    .inputSchema(jsonService.toJson(schema.getInputSchema()))
                    .build();

            ToolCallback callback = new ToolActionCallback(toolDefinition, new ToolService());

            reply = chatClient
                    .prompt(request.message())
                    .options(ChatOptions.builder().model(DEFAULT_MODEL))
                    .tools(callback)
                    .call()
                    .content();
        } else {
            reply = chatClient
                    .prompt(request.message())
                    .options(ChatOptions.builder().model(DEFAULT_MODEL))
                    .call()
                    .content();
        }

        memoryService.appendAssistantMessage(conversationId, reply, DEFAULT_MODEL);

        return new ChatResponse(conversationId, reply);
    }

    @PostMapping("/chat-with-retrieval")
    ChatResponse chatWithRetrieval(@RequestBody ChatRequest request) {
        UUID conversationId = resolveConversationId(request);
        String userId = request.userId() != null ? request.userId() : DEFAULT_USER_ID;

        memoryService.appendUserMessage(conversationId, request.message());

        // Retrieve relevant documents from the vector store
        SearchResponse searchResponse = retrievalService.search(userId, request.message());

        String context = buildContext(searchResponse);

        String augmentedPrompt = context.isBlank()
                ? request.message()
                : """
                  Use the following context to answer the question.

                  Context:
                  %s

                  Question:
                  %s
                  """.formatted(context, request.message());

        String reply = chatClient
                .prompt(augmentedPrompt)
                .options(ChatOptions.builder().model(DEFAULT_MODEL))
                .call()
                .content();

        memoryService.appendAssistantMessage(conversationId, reply, DEFAULT_MODEL);

        return new ChatResponse(conversationId, reply);
    }

    private String buildContext(SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.results() == null || searchResponse.results().isEmpty()) {
            return "";
        }
        return searchResponse.results().stream()
                .filter(r -> r.text() != null && !r.text().isBlank())
                .map(SearchResponse.SearchResultItem::text)
                .collect(Collectors.joining("\n\n"));
    }

    private UUID resolveConversationId(ChatRequest request) {
        if (request.conversationId() != null) {
            return request.conversationId();
        }
        String userId = request.userId() != null ? request.userId() : DEFAULT_USER_ID;
        ConversationResponse conversation = memoryService.createConversation(userId, "agent-service");
        return conversation.id();
    }
}
