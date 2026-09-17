package uz.academixai.intelligence.application;

/** Typed transient failure returned by an Intelligence provider adapter. */
public class AiProviderUnavailableException extends RuntimeException {

  public AiProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
