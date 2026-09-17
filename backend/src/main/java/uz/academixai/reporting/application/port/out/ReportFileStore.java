package uz.academixai.reporting.application.port.out;

/** Outbound port for the object store that keeps generated report PDFs. */
public interface ReportFileStore {

  void upload(String key, byte[] content, String contentType);

  byte[] download(String key);
}
