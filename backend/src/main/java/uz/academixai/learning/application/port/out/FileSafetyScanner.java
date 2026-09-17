package uz.academixai.learning.application.port.out;

/** Security boundary: uploaded bytes must be scanned before persistent storage. */
public interface FileSafetyScanner {

  void scan(byte[] content, String fileName);
}
