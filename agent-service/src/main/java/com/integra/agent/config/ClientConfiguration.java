package com.integra.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClientConfiguration {

    @Bean
    ChatClient defaultChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

}
