package uz.academixai.intelligence.application.port.out;

import java.util.UUID;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.intelligence.domain.OcrDocument;

/** Handwriting-profile policy boundary. */
public interface HandwritingAnalyzer {

  HandwritingCheckResult checkAndUpdate(UUID studentId, OcrDocument document);
}
