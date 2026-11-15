package com.example.ebankbot.discord;

import com.example.ebankbot.agents.EbankAIAgent;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DiscordBot extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private static final int DISCORD_MAX_LENGTH = 2000;

    private final EbankAIAgent ebankAIAgent;
    private final String token;
    private JDA jda;

    public DiscordBot(EbankAIAgent ebankAIAgent, @Value("${discord.token:}") String token) {
        this.ebankAIAgent = ebankAIAgent;
        this.token = token;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (token == null || token.isBlank()) {
            log.warn("discord.token is not set, Discord bot disabled");
            return;
        }
        try {
            jda = JDABuilder.createDefault(token.trim())
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
        } catch (Exception e) {
            log.error("Discord bot disabled: could not log in ({}). Reset the bot token in the Discord Developer Portal.", e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String query = event.getMessage().getContentRaw();
        if (query.isBlank()) return;
        String conversationId = event.getChannel().getId();
        try {
            String response = ebankAIAgent.chat(query, conversationId);
            if (response == null || response.isBlank()) response = "JE NE SAIS PAS";
            for (int i = 0; i < response.length(); i += DISCORD_MAX_LENGTH) {
                event.getChannel()
                        .sendMessage(response.substring(i, Math.min(response.length(), i + DISCORD_MAX_LENGTH)))
                        .queue();
            }
        } catch (Exception e) {
            log.error("Error while handling Discord message", e);
            event.getChannel().sendMessage("Erreur: " + e.getMessage()).queue();
        }
    }

    @PreDestroy
    public void stop() {
        if (jda != null) jda.shutdown();
    }
}
