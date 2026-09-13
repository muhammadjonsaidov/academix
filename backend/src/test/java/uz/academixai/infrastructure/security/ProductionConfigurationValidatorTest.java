package uz.academixai.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {

  @Test
  void rejectsRepositoryDevelopmentDefaults() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("JWT_SECRET", "local-dev-only-placeholder-not-for-production-use");
    environment.setProperty("DATABASE_PASSWORD", "academix");
    environment.setProperty("DATABASE_APP_PASSWORD", "academix_app_local_dev_only");
    environment.setProperty("REDIS_PASSWORD", "");
    environment.setProperty("RABBITMQ_PASSWORD", "academix_local_dev");
    environment.setProperty("SEAWEEDFS_ACCESS_KEY", "local-dev-access-key");
    environment.setProperty("SEAWEEDFS_SECRET_KEY", "local-dev-secret-key");

    assertThrows(
        IllegalStateException.class,
        () -> new ProductionConfigurationValidator(environment).validate());
  }

  @Test
  void acceptsUniqueNonBlankProductionSecrets() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("JWT_SECRET", "a-real-random-production-secret-with-enough-entropy");
    environment.setProperty("DATABASE_PASSWORD", "postgres-production-password");
    environment.setProperty("DATABASE_APP_PASSWORD", "app-production-password");
    environment.setProperty("REDIS_PASSWORD", "redis-production-password");
    environment.setProperty("RABBITMQ_PASSWORD", "rabbit-production-password");
    environment.setProperty("SEAWEEDFS_ACCESS_KEY", "production-s3-access-key");
    environment.setProperty("SEAWEEDFS_SECRET_KEY", "production-s3-secret-key");

    assertDoesNotThrow(() -> new ProductionConfigurationValidator(environment).validate());
  }
}
