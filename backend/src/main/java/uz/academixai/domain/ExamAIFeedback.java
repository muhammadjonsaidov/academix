package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * academix_tz.md §1.23 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ExamAIFeedbackEntity}. No plagiarism fields — exams are proctored on
 * paper, no plagiarism check (unlike {@link AIFeedback}).
 */
public record ExamAIFeedback(
    UUID id,
    UUID schoolId,
    UUID examSubmissionId,
    String extractedText,
    List<StepAnalysis> stepAnalyses,
    List<CriteriaScore> criteriaScores,
    float aiScorePercent,
    String feedback,
    float handwritingMatchScore,
    LocalDateTime processedAt) {}
