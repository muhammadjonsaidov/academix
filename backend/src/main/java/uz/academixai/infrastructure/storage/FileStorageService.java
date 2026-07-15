package uz.academixai.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
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

  private final S3Client s3Client;
  private final StorageProperties properties;

  public FileStorageService(S3Client s3Client, StorageProperties properties) {
    this.s3Client = s3Client;
    this.properties = properties;
  }

  @PostConstruct
  void ensureBucketExists() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
    } catch (NoSuchBucketException e) {
      try {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
      } catch (BucketAlreadyOwnedByYouException ignored) {
        // Another instance created it between our headBucket check and createBucket call.
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
