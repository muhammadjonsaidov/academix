package uz.academixai.infrastructure.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * SeaweedFS is S3-compatible (see CLAUDE.md "Reality checks" — chosen over MinIO for licensing
 * reasons), so a plain AWS SDK v2 {@link S3Client} pointed at its endpoint works with no
 * SeaweedFS-specific SDK. Region is a required SDK param but meaningless here — SeaweedFS ignores
 * it, {@code US_EAST_1} is the conventional placeholder for S3-compatible stores.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3ClientConfig {

  @Bean
  public S3Client s3Client(StorageProperties properties) {
    return S3Client.builder()
        .endpointOverride(URI.create(properties.endpoint()))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
        // SeaweedFS's S3 gateway only supports path-style addressing
        // (bucket.endpoint.com virtual-hosted-style doesn't resolve here).
        .forcePathStyle(true)
        .build();
  }
}
