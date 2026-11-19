package uz.academixai.learning.domain;

/**
 * The school 5-point grade for a percentage score: 86-100 → 5, 71-85 → 4, 56-70 → 3, below → 2.
 * Only a suggestion — the teacher can always pick a different grade.
 */
public final class FivePointScale {

  private FivePointScale() {}

  public static int fromPercent(double percent) {
    long rounded = Math.round(percent);
    if (rounded >= 86) {
      return 5;
    }
    if (rounded >= 71) {
      return 4;
    }
    if (rounded >= 56) {
      return 3;
    }
    return 2;
  }
}
