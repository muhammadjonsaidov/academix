package uz.academixai.infrastructure.security;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to run a production process with repository-local development credentials.
 *
 * <p>This is deliberately profile-scoped rather than a Docker-only check: Railway, an IDE launch
 * configuration, and any future orchestrator must get the same protection as Docker Compose. The
 * values are never logged; the exception names only the invalid configuration keys.
 */
@Component
@Profile("prod")
public class ProductionConfigurationValidator {

  private static final Map<String, String> FORBIDDEN_DEFAULTS = forbiddenDefaults();

  private final Environment environment;

  public ProductionConfigurationValidator(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    StringBuilder invalidKeys = new StringBuilder();
    FORBIDDEN_DEFAULTS.forEach(
        (key, forbiddenValue) -> {
          String configured = environment.getProperty(key);
          if (configured == null || configured.isBlank() || forbiddenValue.equals(configured)) {
            if (!invalidKeys.isEmpty()) {
              invalidKeys.append(", ");
            }
            invalidKeys.append(key);
          }
        });

    if (!invalidKeys.isEmpty()) {
      throw new IllegalStateException(
          "Production startup refused: missing or unsafe development defaults for "
              + invalidKeys
              + ". Set unique production secrets before deploying.");
    }
  }

  private static Map<String, String> forbiddenDefaults() {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("JWT_SECRET", "local-dev-only-placeholder-not-for-production-use");
    values.put("DATABASE_PASSWORD", "academix");
    values.put("DATABASE_APP_PASSWORD", "academix_app_local_dev_only");
    values.put("REDIS_PASSWORD", "");
    values.put("RABBITMQ_PASSWORD", "academix_local_dev");
    values.put("SEAWEEDFS_ACCESS_KEY", "local-dev-access-key");
    values.put("SEAWEEDFS_SECRET_KEY", "local-dev-secret-key");
    return Map.copyOf(values);
  }
}
