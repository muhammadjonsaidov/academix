package uz.academixai.infrastructure.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.telegram")
// botToken/webhookSecret dropped — backend no longer calls the Telegram API or receives a
// webhook at all; the bot moved to a standalone long-polling service (telegram-bot/, which has
// its own copy of botToken). Backend only needs botUsername, to build the deep-link URL
// (https://t.me/{username}?start=...) in TelegramLinkService.generateLinkToken.
public record TelegramProperties(String botUsername) {}
