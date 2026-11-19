package uz.academixai.shared.ai;

/**
 * How a piece of written work is checked. Every school subject maps to one of these (see
 * intelligence's {@code GradingModes}); each mode has its own grading prompt and default rubric.
 */
public enum GradingMode {
  /** Math, physics, chemistry: method, steps and the final answer. */
  PROBLEM_SOLVING,
  /** Native and foreign languages: spelling, grammar, punctuation, vocabulary, structure. */
  LANGUAGE,
  /** History, biology, geography, literature and the rest: facts, completeness, reasoning. */
  WRITTEN_RESPONSE
}
