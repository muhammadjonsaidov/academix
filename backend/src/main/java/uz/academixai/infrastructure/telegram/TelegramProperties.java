package uz.academixai.infrastructure.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.telegram")
public record TelegramProperties(String botToken, String botUsername, String webhookSecret) {}
