package com.example.ebankbot.telegram;

import com.example.ebankbot.agents.EbankAIAgent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private static final int TELEGRAM_MAX_LENGTH = 4096;

    private final EbankAIAgent aiAgent;
    private final String telegramBotToken;
    private TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;

    public TelegramBot(EbankAIAgent aiAgent, @Value("${telegram.token:}") String telegramBotToken) {
        this.aiAgent = aiAgent;
        this.telegramBotToken = telegramBotToken == null ? "" : telegramBotToken.trim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerTelegramBot() {
        if (telegramBotToken.isBlank()) {
            log.warn("telegram.token is not set, Telegram bot disabled");
            return;
        }
        try {
            telegramClient = new OkHttpTelegramClient(telegramBotToken);
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(telegramBotToken, this);
            log.info("Telegram bot started");
        } catch (Exception e) {
            log.error("Telegram bot disabled: could not start ({}). Check telegram.token (from @BotFather).", e.getMessage());
        }
    }

    @Override
    public void consume(Update telegramRequest) {
        if (!telegramRequest.hasMessage()) return;
        Message message = telegramRequest.getMessage();
        Long chatId = message.getChatId();
        try {
            String query = message.hasText() ? message.getText() : message.getCaption();
            List<Media> mediaList = new ArrayList<>();

            if (message.hasPhoto()) {
                if (query == null || query.isBlank()) query = "Que vois-tu dans cette image ?";
                // Telegram sends several sizes of the same photo: keep the biggest one
                PhotoSize photo = message.getPhoto().stream()
                        .max(Comparator.comparing(p -> p.getFileSize() == null ? 0 : p.getFileSize()))
                        .orElseThrow();
                File file = telegramClient.execute(new GetFile(photo.getFileId()));
                try (InputStream in = telegramClient.downloadFileAsStream(file)) {
                    mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(in.readAllBytes())));
                }
            }

            if (query == null || query.isBlank()) return;

            String response = aiAgent.chat(query, mediaList, "telegram-" + chatId);
            if (response == null || response.isBlank()) response = "JE NE SAIS PAS";
            send(chatId, response);
        } catch (Exception e) {
            log.error("Error while handling Telegram message", e);
            try {
                send(chatId, "Erreur: " + e.getMessage());
            } catch (TelegramApiException ignored) {
            }
        }
    }

    private void send(Long chatId, String text) throws TelegramApiException {
        for (int i = 0; i < text.length(); i += TELEGRAM_MAX_LENGTH) {
            String part = text.substring(i, Math.min(text.length(), i + TELEGRAM_MAX_LENGTH));
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(part).build());
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (botsApplication != null) botsApplication.close();
    }
}
