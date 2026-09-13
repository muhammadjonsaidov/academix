package uz.academixai.intelligence.infrastructure.handwriting;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.HandwritingService;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.infrastructure.ai.CharacterBox;
import uz.academixai.infrastructure.ai.DocumentTextLayout;
import uz.academixai.intelligence.application.port.out.HandwritingAnalyzer;
import uz.academixai.intelligence.domain.OcrCharacter;
import uz.academixai.intelligence.domain.OcrDocument;

/** Compatibility adapter while handwriting profile persistence is migrated into Intelligence. */
@Component
public class LegacyHandwritingAnalyzer implements HandwritingAnalyzer {

  private final HandwritingService handwriting;

  public LegacyHandwritingAnalyzer(HandwritingService handwriting) {
    this.handwriting = handwriting;
  }

  @Override
  public HandwritingCheckResult checkAndUpdate(UUID studentId, OcrDocument document) {
    List<CharacterBox> characters =
        document.characters().stream()
            .map(
                character ->
                    new CharacterBox(
                        character.text(), character.vertices(), mapBreak(character.breakAfter())))
            .toList();
    return handwriting.checkAndUpdateProfile(studentId, new DocumentTextLayout(characters));
  }

  private static CharacterBox.BreakType mapBreak(OcrCharacter.BreakType value) {
    return switch (value) {
      case NONE -> CharacterBox.BreakType.NONE;
      case SPACE -> CharacterBox.BreakType.SPACE;
      case LINE_BREAK -> CharacterBox.BreakType.LINE_BREAK;
    };
  }
}
