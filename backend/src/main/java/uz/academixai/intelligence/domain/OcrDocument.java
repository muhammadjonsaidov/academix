package uz.academixai.intelligence.domain;

import java.util.List;

/** Provider-neutral OCR result used by grading and handwriting policies. */
public record OcrDocument(String extractedText, List<OcrCharacter> characters) {}
