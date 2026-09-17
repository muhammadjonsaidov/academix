package uz.academixai.intelligence.infrastructure.storage;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.intelligence.application.port.out.SubmissionImageStore;

/** SeaweedFS adapter for private submission image reads. */
@Component
public class SeaweedSubmissionImageStore implements SubmissionImageStore {

  private final FileStorageService storage;

  public SeaweedSubmissionImageStore(FileStorageService storage) {
    this.storage = storage;
  }

  @Override
  public byte[] download(String key) {
    return storage.download(key);
  }
}
