package uz.academixai.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Homework submission photos (and later syllabus PDFs) live here, not in Postgres. */
@Service
public class FileStorageService {

  private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

  // Deploy environments with scale-to-zero (Railway trial's mandatory App Sleeping) routinely have
  // SeaweedFS asleep at backend boot; the first S3 call is exactly what wakes it, but the wake
  // takes seconds — a single fail-fast attempt here killed the whole Spring context and forced a
  // full ~60s boot-restart cycle per race (confirmed by real Railway deploy logs). 12 x 5s covers
  // any realistic wake latency; a genuinely down/misconfigured SeaweedFS still fails the boot,
  // just a minute later.
  private static final int BUCKET_CHECK_MAX_ATTEMPTS = 12;
  private static final long BUCKET_CHECK_RETRY_DELAY_MS = 5_000;

  private final S3Client s3Client;
  private final StorageProperties properties;

  public FileStorageService(S3Client s3Client, StorageProperties properties) {
    this.s3Client = s3Client;
    this.properties = properties;
  }

  @PostConstruct
  void ensureBucketExists() {
    for (int attempt = 1; ; attempt++) {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
        return;
      } catch (NoSuchBucketException e) {
        try {
          s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (BucketAlreadyOwnedByYouException ignored) {
          // Another instance created it between our headBucket check and createBucket call.
        }
        return;
      } catch (SdkException e) {
        if (attempt >= BUCKET_CHECK_MAX_ATTEMPTS) {
          throw e;
        }
        log.warn(
            "SeaweedFS unreachable on bucket check (attempt {}/{}), retrying in {}ms — likely"
                + " waking from sleep: {}",
            attempt,
            BUCKET_CHECK_MAX_ATTEMPTS,
            BUCKET_CHECK_RETRY_DELAY_MS,
            e.getMessage());
        try {
          Thread.sleep(BUCKET_CHECK_RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for SeaweedFS", ie);
        }
      }
    }
  }

  public void upload(String key, byte[] content, String contentType) {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(key)
            .contentType(contentType)
            .build(),
        RequestBody.fromBytes(content));
  }

  public byte[] download(String key) {
    try (ResponseInputStream<GetObjectResponse> response =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(properties.bucket()).key(key).build())) {
      return response.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
