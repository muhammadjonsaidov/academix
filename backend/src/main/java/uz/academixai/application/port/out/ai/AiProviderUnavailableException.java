package uz.academixai.application.port.out.ai;

/** Signals that an AI provider call cannot complete. */
public class AiProviderUnavailableException extends RuntimeException {
  public AiProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
