package uz.academixai.intelligence.infrastructure.ocr;

import java.util.List;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.ai.CharacterBox;
import uz.academixai.infrastructure.ai.GoogleVisionClient;
import uz.academixai.infrastructure.ai.OcrUnavailableException;
import uz.academixai.intelligence.application.port.out.OcrGateway;
import uz.academixai.intelligence.domain.OcrCharacter;
import uz.academixai.intelligence.domain.OcrDocument;

/** Google Vision adapter that maps provider geometry to the Intelligence OCR contract. */
@Component
public class GoogleVisionOcrGateway implements OcrGateway {

  private final GoogleVisionClient vision;

  public GoogleVisionOcrGateway(GoogleVisionClient vision) {
    this.vision = vision;
  }

  @Override
  public OcrDocument extract(byte[] image) {
    try {
      var result = vision.extractText(image);
      List<OcrCharacter> characters =
          result.layout().characters().stream()
              .map(
                  character ->
                      new OcrCharacter(
                          character.text(), character.vertices(), mapBreak(character.breakAfter())))
              .toList();
      return new OcrDocument(result.extractedText(), characters);
    } catch (OcrUnavailableException exception) {
      throw new uz.academixai.intelligence.application.OcrUnavailableException(
          "Google Vision OCR unavailable", exception);
    }
  }

  private static OcrCharacter.BreakType mapBreak(CharacterBox.BreakType value) {
    return switch (value) {
      case NONE -> OcrCharacter.BreakType.NONE;
      case SPACE -> OcrCharacter.BreakType.SPACE;
      case LINE_BREAK -> OcrCharacter.BreakType.LINE_BREAK;
    };
  }
}
