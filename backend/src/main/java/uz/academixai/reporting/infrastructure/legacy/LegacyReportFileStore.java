package uz.academixai.reporting.infrastructure.legacy;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.reporting.application.port.out.ReportFileStore;

/** Adapter for {@link ReportFileStore} over the legacy S3-backed {@code FileStorageService}. */
@Component
public class LegacyReportFileStore implements ReportFileStore {

  private final FileStorageService files;

  public LegacyReportFileStore(FileStorageService files) {
    this.files = files;
  }

  @Override
  public void upload(String key, byte[] content, String contentType) {
    files.upload(key, content, contentType);
  }

  @Override
  public byte[] download(String key) {
    return files.download(key);
  }
}
