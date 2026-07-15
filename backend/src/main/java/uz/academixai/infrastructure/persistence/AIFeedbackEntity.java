package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.StepAnalysis;

/**
 * JPA mapping for {@code ai_feedbacks} (backend_tdd.md §4.1 table 10). Maps to/from {@link
 * AIFeedback}.
 */
@Entity
@Table(name = "ai_feedbacks")
public class AIFeedbackEntity {

  @Id private UUID id;

  @Column(name = "submission_id", nullable = false, unique = true)
  private UUID submissionId;

  @Column(name = "extracted_text")
  private String extractedText;

  @Column(name = "ocr_confidence")
  private float ocrConfidence;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "step_analyses")
  private List<StepAnalysis> stepAnalyses;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "criteria_scores")
  private List<CriteriaScore> criteriaScores;

  @Column(name = "ai_score_percent")
  private float aiScorePercent;

  private String feedback;

  @Column(name = "highlighted_errors")
  private String highlightedErrors;

  @Column(name = "plagiarism_score")
  private float plagiarismScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "plagiarism_type")
  private PlagiarismType plagiarismType;

  @Column(name = "handwriting_match_score")
  private float handwritingMatchScore;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  protected AIFeedbackEntity() {}

  public AIFeedbackEntity(
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
      LocalDateTime processedAt) {
    this.id = id;
    this.submissionId = submissionId;
    this.extractedText = extractedText;
    this.ocrConfidence = ocrConfidence;
    this.stepAnalyses = stepAnalyses;
    this.criteriaScores = criteriaScores;
    this.aiScorePercent = aiScorePercent;
    this.feedback = feedback;
    this.highlightedErrors = highlightedErrors;
    this.plagiarismScore = plagiarismScore;
    this.plagiarismType = plagiarismType;
    this.handwritingMatchScore = handwritingMatchScore;
    this.processedAt = processedAt;
  }

  public static AIFeedbackEntity fromDomain(AIFeedback domain) {
    return new AIFeedbackEntity(
        domain.id(),
        domain.submissionId(),
        domain.extractedText(),
        domain.ocrConfidence(),
        domain.stepAnalyses(),
        domain.criteriaScores(),
        domain.aiScorePercent(),
        domain.feedback(),
        domain.highlightedErrors(),
        domain.plagiarismScore(),
        domain.plagiarismType(),
        domain.handwritingMatchScore(),
        domain.processedAt());
  }

  public AIFeedback toDomain() {
    return new AIFeedback(
        id,
        submissionId,
        extractedText,
        ocrConfidence,
        stepAnalyses,
        criteriaScores,
        aiScorePercent,
        feedback,
        highlightedErrors,
        plagiarismScore,
        plagiarismType,
        handwritingMatchScore,
        processedAt);
  }

  public UUID getSubmissionId() {
    return submissionId;
  }
}
