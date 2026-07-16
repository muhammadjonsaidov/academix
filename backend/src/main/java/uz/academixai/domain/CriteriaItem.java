package uz.academixai.domain;

/**
 * academix_tz.md §1.19 — one entry in {@code SubjectGradingCriteria.criteria}. Distinct from {@link
 * CriteriaScore}: this is the teacher-configured weight/description, not an AI-produced score.
 */
public record CriteriaItem(String name, int weightPercent, String description) {}
