package uz.academixai.shared.ai;

import uz.academixai.domain.SubjectType;

/**
 * How a piece of written work is checked. Every school subject maps to one of these; each mode has
 * its own grading prompt and default rubric (intelligence's {@code GradingModes}).
 */
public enum GradingMode {
  /** Math, physics, chemistry: method, steps and the final answer. */
  PROBLEM_SOLVING,
  /** Native and foreign languages: spelling, grammar, punctuation, vocabulary, structure. */
  LANGUAGE,
  /** History, biology, geography, literature and the rest: facts, completeness, reasoning. */
  WRITTEN_RESPONSE;

  public static GradingMode forSubject(SubjectType type) {
    if (type == null) {
      return WRITTEN_RESPONSE;
    }
    return switch (type) {
      case MATH, PHYSICS, CHEMISTRY -> PROBLEM_SOLVING;
      case LANGUAGE_UZ, LANGUAGE_RU, LANGUAGE_EN -> LANGUAGE;
      case BIOLOGY, HISTORY, GEOGRAPHY, OTHER -> WRITTEN_RESPONSE;
    };
  }
}
