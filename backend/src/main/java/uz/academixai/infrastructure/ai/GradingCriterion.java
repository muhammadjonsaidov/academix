package uz.academixai.infrastructure.ai;

/** Input to {@link QwenAIClient#gradeSubmission} — from SubjectGradingCriteria (TZ §1.19). */
public record GradingCriterion(String name, int weightPercent) {}
