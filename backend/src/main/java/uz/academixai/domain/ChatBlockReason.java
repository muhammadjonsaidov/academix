package uz.academixai.domain;

/**
 * academix_tz.md §2.3/§3.4 — {@code IRRELEVANT_QUESTION}/{@code POTENTIAL_ANSWER_LEAK} are spec'd
 * exactly. {@code BUDGET_EXHAUSTED} is a deviation: the spec's degradation order (backend TDD "AI
 * cost/budget system") says chat is blocked first when a school's budget runs out, but no
 * blockReason value is given for that case — the two spec'd reasons are both about content policy,
 * not budget. Needed so a budget-exhausted chat request still gets a real response shape instead of
 * an undefined one.
 */
public enum ChatBlockReason {
  IRRELEVANT_QUESTION,
  POTENTIAL_ANSWER_LEAK,
  BUDGET_EXHAUSTED
}
