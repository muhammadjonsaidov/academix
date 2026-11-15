package uz.academixai.domain;

/** academix_tz.md §1.11 */
public enum PlagiarismType {
  CLEAN,
  AI_GENERATED,
  /** Retired with handwriting biometrics — never produced any more, kept so old rows still load. */
  HANDWRITING_MISMATCH,
  SUSPICIOUS
}
