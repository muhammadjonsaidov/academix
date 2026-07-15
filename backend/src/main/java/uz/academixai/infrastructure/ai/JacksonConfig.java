package uz.academixai.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Boot 4.1 defaults to Jackson 3 ({@code tools.jackson.*}) as its primary JSON engine — {@code
 * JacksonAutoConfiguration} wires a {@code tools.jackson.databind.json.JsonMapper} bean, not a
 * legacy {@code com.fasterxml.jackson.databind.ObjectMapper} one. Confirmed by a real startup
 * failure the moment {@link QwenAIClient} tried to inject the legacy type directly ("No qualifying
 * bean of type ObjectMapper"), even though the legacy Jackson 2 jar is still on the classpath
 * (pulled in transitively, e.g. by springdoc/jackson-dataformat-yaml, neither of which has migrated
 * to Jackson 3 yet) and {@code RestClient.body(JsonNode.class)} elsewhere in this codebase
 * (GoogleVisionClient) keeps working fine via message-converter fallback. Rather than migrate every
 * AI-client DTO to Jackson 3's new package (a much bigger, riskier change for marginal benefit
 * right now), this restores a plain legacy {@code ObjectMapper} bean explicitly.
 */
@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
