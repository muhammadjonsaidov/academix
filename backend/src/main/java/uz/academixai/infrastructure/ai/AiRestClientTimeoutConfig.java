package uz.academixai.infrastructure.ai;

import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Boot's default {@code RestClient.Builder} has no read timeout (confirmed: no {@code
 * RestClientCustomizer} bean existed anywhere before this) — harmless for Google Vision/Telegram
 * (fast calls), but a real problem for Qwen: {@code qwen3.7-max} has extended "reasoning" mode on,
 * confirmed by a real request taking 52.9s (2218 of 2859 completion tokens were {@code
 * reasoning_content} thinking tokens before the actual JSON answer) — 10x over the circuit
 * breaker's 5s slow-call-duration-threshold. Without an explicit timeout the call would otherwise
 * hang indefinitely on a genuine network stall; 90s gives real Qwen calls room to finish.
 *
 * <p>Real Boot 4.1 API, verified against the actual jars (this project's history shows Boot 4.1
 * moved several package paths from 3.x) — {@code ClientHttpRequestFactorySettings} doesn't exist in
 * 4.1, {@code HttpClientSettings} (org.springframework.boot.http.client) replaces it.
 */
@Configuration
public class AiRestClientTimeoutConfig {

  @Bean
  public RestClientCustomizer aiRestClientTimeoutCustomizer() {
    return builder ->
        builder.requestFactory(
            ClientHttpRequestFactoryBuilder.detect()
                .build(
                    HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withReadTimeout(Duration.ofSeconds(90))));
  }
}
