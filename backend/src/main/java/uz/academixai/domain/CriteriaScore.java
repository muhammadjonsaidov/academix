package uz.academixai.domain;

/**
 * academix_tz.md §1.11/§3.2 — one entry per grading-criterion score, stored as part of {@code
 * ai_feedbacks.criteria_scores} JSONB and echoed into a {@code Grade} at grading time.
 */
public record CriteriaScore(String name, int weightPercent, int score) {}
