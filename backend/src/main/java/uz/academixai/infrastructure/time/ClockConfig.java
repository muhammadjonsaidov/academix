package uz.academixai.infrastructure.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application clock. System default zone on purpose: every stored LocalDateTime in this
 * codebase comes from LocalDateTime.now(), so comparisons must use the same zone. Tests inject a
 * fixed clock instead.
 */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
