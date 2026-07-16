package uz.academixai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling — first @Scheduled job in this codebase (HandwritingAnomalyService,
// backend_tdd.md §6.5) needed this added; without it, @Scheduled annotations are silently inert
// (no error, the method just never fires).
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
