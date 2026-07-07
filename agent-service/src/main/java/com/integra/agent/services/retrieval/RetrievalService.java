package com.integra.agent.services.retrieval;

import com.integra.agent.integration.retrieval.RetrievalServiceAPI;
import com.integra.agent.integration.retrieval.dto.DocumentRequest;
import com.integra.agent.integration.retrieval.dto.DocumentResponse;
import com.integra.agent.integration.retrieval.dto.SearchRequest;
import com.integra.agent.integration.retrieval.dto.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RetrievalService {

    private final RetrievalServiceAPI retrievalServiceAPI;

    public RetrievalService(RetrievalServiceAPI retrievalServiceAPI) {
        this.retrievalServiceAPI = retrievalServiceAPI;
    }

    public DocumentResponse addDocument(String userId, String text) {
        return retrievalServiceAPI.addDocument(userId, new DocumentRequest(text));
    }

    public DocumentResponse addDocument(String userId, String text, Map<String, Object> metadata) {
        return retrievalServiceAPI.addDocument(userId, new DocumentRequest(text, metadata));
    }

    public SearchResponse search(String userId, String query) {
        return retrievalServiceAPI.search(userId, new SearchRequest(query));
    }

    public SearchResponse search(String userId, String query, int limit) {
        return retrievalServiceAPI.search(userId, new SearchRequest(query, limit));
    }
}
