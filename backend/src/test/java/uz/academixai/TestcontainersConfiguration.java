package uz.academixai;

import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    // Plain postgres images don't bundle pgvector (confirmed by a real failed migration
    // during Sprint 0 — see CLAUDE.md "Reality checks"). Must use the pgvector variant.
    return new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));
  }

  @Bean
  @ServiceConnection
  RabbitMQContainer rabbitContainer() {
    return new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));
  }

  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
  }

  @Bean
  GenericContainer<?> seaweedFsContainer() {
    return new GenericContainer<>(DockerImageName.parse("chrislusf/seaweedfs:latest"))
        .withExposedPorts(8333)
        .withCommand("server", "-s3", "-s3.config=/etc/seaweedfs/s3-config.json", "-dir=/data")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("seaweedfs/s3-config.json"),
            "/etc/seaweedfs/s3-config.json");
  }

  /** Uses the test S3 endpoint instead of application.yml's localhost development endpoint. */
  @Bean
  @Primary
  S3Client testS3Client(@Qualifier("seaweedFsContainer") GenericContainer<?> seaweedFs) {
    return S3Client.builder()
        .endpointOverride(
            URI.create("http://" + seaweedFs.getHost() + ":" + seaweedFs.getMappedPort(8333)))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("local-dev-access-key", "local-dev-secret-key")))
        .forcePathStyle(true)
        .build();
  }
}
