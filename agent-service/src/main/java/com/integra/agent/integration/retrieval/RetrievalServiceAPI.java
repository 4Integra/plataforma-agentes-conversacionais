package com.integra.agent.integration.retrieval;

import com.integra.agent.integration.retrieval.dto.DocumentRequest;
import com.integra.agent.integration.retrieval.dto.DocumentResponse;
import com.integra.agent.integration.retrieval.dto.SearchRequest;
import com.integra.agent.integration.retrieval.dto.SearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "retrievalService", url = "${retrieval.base.url}")
public interface RetrievalServiceAPI {

    @PostMapping("/documents")
    DocumentResponse addDocument(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody DocumentRequest request
    );

    @PostMapping("/search")
    SearchResponse search(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchRequest request
    );
}
