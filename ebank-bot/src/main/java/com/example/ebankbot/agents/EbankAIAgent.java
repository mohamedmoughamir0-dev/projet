package com.example.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EbankAIAgent {
    private final ChatClient chatClient;

    public EbankAIAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Vous un assistant qui se charge de répondre aux question
                        de l'utilisateur  a propos des clients et des comptes bancaires en fonction du contexte fourni.
                        Sii aucun context n'est fournis ,réponds avec JE NE SAIS PAS
                        
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(tools.getToolCallbacks())
                .build();
    }

    public String chat(String query, String conversationId) {
        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    public String chat(String query, List<Media> media, String conversationId) {
        if (media == null || media.isEmpty()) return chat(query, conversationId);
        return chatClient.prompt()
                .user(u -> u.text(query).media(media.toArray(new Media[0])))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
