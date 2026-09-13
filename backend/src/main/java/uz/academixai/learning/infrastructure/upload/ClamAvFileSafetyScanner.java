package uz.academixai.learning.infrastructure.upload;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.antivirus.ClamAvScanner;
import uz.academixai.learning.application.port.out.FileSafetyScanner;

/** Antivirus adapter for Learning upload commands. */
@Component
public class ClamAvFileSafetyScanner implements FileSafetyScanner {

  private final ClamAvScanner scanner;

  public ClamAvFileSafetyScanner(ClamAvScanner scanner) {
    this.scanner = scanner;
  }

  @Override
  public void scan(byte[] content, String fileName) {
    scanner.scan(content, fileName);
  }
}
