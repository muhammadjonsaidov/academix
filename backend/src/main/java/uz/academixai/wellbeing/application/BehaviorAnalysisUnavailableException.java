package uz.academixai.wellbeing.application;

/** Typed transient failure returned by a Wellbeing behavioral-analysis adapter. */
public class BehaviorAnalysisUnavailableException extends RuntimeException {

  public BehaviorAnalysisUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
