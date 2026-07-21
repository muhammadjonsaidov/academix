package uz.academixai.telegrambot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring-amqp's {@code Jackson2JsonMessageConverter} is tied to the legacy {@code
 * com.fasterxml.jackson.databind} package regardless of Boot 4.1 defaulting to Jackson 3 elsewhere
 * — Boot's autoconfiguration doesn't provide a legacy {@code ObjectMapper} bean automatically
 * (confirmed by backend/'s own {@code JacksonConfig}, hit the exact same gap first). This is the
 * one place in this service that still needs the legacy package — {@link TelegramApiClient} uses
 * Jackson 3 directly for everything else, since this is a fresh service with no legacy baggage.
 */
@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
