package uz.academixai.infrastructure.ai;

/**
 * Thrown when Qwen is unreachable/circuit-open. Unlike {@link OcrUnavailableException}, this is NOT
 * a hard failure for the pipeline — academix_tz.md §8's degradation order puts homework grading
 * second (after chat, before exams): the submission is still accepted, OCR still runs, status
 * becomes {@code AI_SKIPPED}, and a teacher grades manually. The queue consumer catches this
 * specifically to take that path rather than failing the whole message.
 */
public class QwenUnavailableException extends RuntimeException {
  public QwenUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
