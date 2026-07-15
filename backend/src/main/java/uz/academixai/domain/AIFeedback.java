package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * academix_tz.md §1.11 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.AIFeedbackEntity}. {@code highlightedErrors} (a JSON array of {@code
 * {line, error, suggestion}}) isn't populated by the current Qwen prompt (§3.2's response shape has
 * no per-line error list, only {@code stepAnalyses}) — left as a raw JSON string, null until a
 * future prompt revision adds it.
 */
public record AIFeedback(
    UUID id,
    UUID submissionId,
    String extractedText,
    float ocrConfidence,
    List<StepAnalysis> stepAnalyses,
    List<CriteriaScore> criteriaScores,
    float aiScorePercent,
    String feedback,
    String highlightedErrors,
    float plagiarismScore,
    PlagiarismType plagiarismType,
    float handwritingMatchScore,
    LocalDateTime processedAt) {}
