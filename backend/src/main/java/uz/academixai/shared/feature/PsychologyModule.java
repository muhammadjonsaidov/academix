package uz.academixai.shared.feature;

/**
 * The psychological-monitoring add-on module (psychological signals, the Psychologist role, the
 * nightly behaviour analysis). Sold separately and off by default; one property switches it.
 */
public final class PsychologyModule {

  /** Bound to env {@code ACADEMIX_FEATURE_PSYCHOLOGY} in application.yml. */
  public static final String ENABLED_PROPERTY = "academix.features.psychology.enabled";

  /** For {@code @Value}: the flag, {@code false} when unset. */
  public static final String ENABLED_VALUE = "${" + ENABLED_PROPERTY + ":false}";

  private PsychologyModule() {}
}
