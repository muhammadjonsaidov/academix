package uz.academixai.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.storage.seaweedfs")
public record StorageProperties(
    String endpoint, String accessKey, String secretKey, String bucket) {}
