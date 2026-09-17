package uz.academixai.shared.ai;

/** Signals that an AI provider call cannot complete. */
public class AiProviderUnavailableException extends RuntimeException {
  public AiProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
