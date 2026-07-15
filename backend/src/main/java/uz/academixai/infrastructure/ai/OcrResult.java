package uz.academixai.infrastructure.ai;

/**
 * academix_tz.md §3.1 — only {@code fullTextAnnotation.text} for now. Bounding-box/layout data
 * (per-block/paragraph coordinates) is also in the real Vision response but unused until the
 * handwriting-biometrics sprint; not modeled here to avoid parsing a shape nothing reads yet.
 */
public record OcrResult(String extractedText) {}
