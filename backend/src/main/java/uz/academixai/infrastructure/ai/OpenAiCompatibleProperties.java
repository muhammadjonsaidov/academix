package uz.academixai.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings shared by any provider that implements OpenAI's chat-completions API. */
@ConfigurationProperties(prefix = "academix.ai.openai-compatible")
public record OpenAiCompatibleProperties(
    String apiKey, String baseUrl, String modelText, String modelVision) {}
