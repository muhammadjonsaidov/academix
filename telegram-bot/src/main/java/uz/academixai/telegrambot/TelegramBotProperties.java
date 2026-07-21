package uz.academixai.telegrambot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mirrors backend's {@code academix.telegram.*} properties — same env vars, both services need the
 * same bot identity (backend issues link-tokens/deep-links, this service is the actual bot).
 */
@ConfigurationProperties(prefix = "academix.telegram")
public record TelegramBotProperties(String botToken, String botUsername) {}
