package uz.academixai.infrastructure.ai;

/**
 * Thrown when Google Vision is unreachable/circuit-open. Unlike a Qwen failure (which degrades to
 * {@code AI_SKIPPED} — grading skipped, submission still accepted), OCR is a hard dependency: with
 * no extracted text there is nothing to grade. The queue consumer catches this to route the
 * submission the same way it would AI_SKIPPED (accepted, flagged for manual review) rather than
 * failing the whole message.
 */
public class OcrUnavailableException extends RuntimeException {
  public OcrUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
