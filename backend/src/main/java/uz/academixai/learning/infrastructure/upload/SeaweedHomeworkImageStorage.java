package uz.academixai.learning.infrastructure.upload;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.learning.application.port.out.HomeworkImageStorage;

/** SeaweedFS-backed homework image adapter. */
@Component
public class SeaweedHomeworkImageStorage implements HomeworkImageStorage {

  private final FileStorageService storage;

  public SeaweedHomeworkImageStorage(FileStorageService storage) {
    this.storage = storage;
  }

  @Override
  public void store(String key, byte[] content, String contentType) {
    storage.upload(key, content, contentType);
  }
}
