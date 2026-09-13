package uz.academixai.infrastructure.ai;

/**
 * Thrown when the configured AI provider is unreachable or its circuit is open. Unlike {@link
 * OcrUnavailableException}, this is NOT a hard failure for the pipeline — academix_tz.md §8's
 * degradation order puts homework grading second (after chat, before exams): the submission is
 * still accepted, OCR still runs, status becomes {@code AI_SKIPPED}, and a teacher grades manually.
 * The queue consumer catches this specifically to take that path rather than failing the whole
 * message.
 */
public class AiProviderUnavailableException extends RuntimeException {
  public AiProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
