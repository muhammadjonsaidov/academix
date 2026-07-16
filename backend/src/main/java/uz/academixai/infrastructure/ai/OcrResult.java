package uz.academixai.infrastructure.ai;

/**
 * academix_tz.md §3.1 — {@code fullTextAnnotation.text} plus, since Sprint 5, the same response's
 * symbol-level bounding-box layout ({@link DocumentTextLayout}), needed for handwriting biometrics
 * (§1.13). One Vision call produces both — no reason to spend a second API call (and second
 * AI-budget deduction) just to also get the layout.
 */
public record OcrResult(String extractedText, DocumentTextLayout layout) {}
