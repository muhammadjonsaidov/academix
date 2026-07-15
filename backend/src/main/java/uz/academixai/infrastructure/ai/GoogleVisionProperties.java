package uz.academixai.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.ai.google-vision")
public record GoogleVisionProperties(String apiKey) {}
