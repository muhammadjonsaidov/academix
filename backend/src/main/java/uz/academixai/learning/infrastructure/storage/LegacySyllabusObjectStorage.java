package uz.academixai.learning.infrastructure.storage;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.learning.application.port.out.SyllabusObjectStorage;

/**
 * Compatibility adapter over the legacy {@link FileStorageService} (SeaweedFS/S3). It disappears
 * when object storage moves behind a shared published port; submissions already have their own
 * narrow storage ports ({@code HomeworkImageStorage}/{@code ExamImageStorage}).
 */
@Component
public class LegacySyllabusObjectStorage implements SyllabusObjectStorage {

  private final FileStorageService storage;

  public LegacySyllabusObjectStorage(FileStorageService storage) {
    this.storage = storage;
  }

  @Override
  public void upload(String objectKey, byte[] content, String contentType) {
    storage.upload(objectKey, content, contentType);
  }

  @Override
  public byte[] download(String objectKey) {
    return storage.download(objectKey);
  }
}
