package uz.academixai.learning.infrastructure.upload;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.learning.application.port.out.ExamImageStorage;

/** SeaweedFS adapter for scanned exam-paper images. */
@Component
public class SeaweedExamImageStorage implements ExamImageStorage {

  private final FileStorageService storage;

  public SeaweedExamImageStorage(FileStorageService storage) {
    this.storage = storage;
  }

  @Override
  public void store(String key, byte[] content, String contentType) {
    storage.upload(key, content, contentType);
  }
}
