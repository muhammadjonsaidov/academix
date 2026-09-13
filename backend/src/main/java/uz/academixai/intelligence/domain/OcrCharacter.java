package uz.academixai.intelligence.domain;

import java.util.List;

/** Provider-neutral character geometry retained from document OCR for handwriting analysis. */
public record OcrCharacter(String text, List<double[]> vertices, BreakType breakAfter) {

  public enum BreakType {
    NONE,
    SPACE,
    LINE_BREAK
  }
}
