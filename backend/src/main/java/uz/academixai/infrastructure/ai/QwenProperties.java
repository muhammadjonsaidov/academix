package uz.academixai.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.ai.qwen")
public record QwenProperties(String apiKey, String baseUrl, String modelText, String modelVision) {}
