package uz.academixai.intelligence.application;

/** Typed transient failure returned by an OCR adapter. */
public class OcrUnavailableException extends RuntimeException {

  public OcrUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
