package uz.academixai.intelligence.infrastructure.ocr;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.ai.GoogleVisionClient;
import uz.academixai.infrastructure.ai.OcrUnavailableException;
import uz.academixai.intelligence.application.port.out.OcrGateway;

/** Google Vision adapter for the Intelligence OCR contract. */
@Component
public class GoogleVisionOcrGateway implements OcrGateway {

  private final GoogleVisionClient vision;

  public GoogleVisionOcrGateway(GoogleVisionClient vision) {
    this.vision = vision;
  }

  @Override
  public String extract(byte[] image) {
    try {
      return vision.extractText(image);
    } catch (OcrUnavailableException exception) {
      throw new uz.academixai.intelligence.application.OcrUnavailableException(
          "Google Vision OCR unavailable", exception);
    }
  }
}
