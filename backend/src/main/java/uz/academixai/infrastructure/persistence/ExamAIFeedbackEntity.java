package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.StepAnalysis;

/**
 * JPA mapping for {@code exam_ai_feedbacks} (backend_tdd.md §4.1 table 18). Maps to/from {@link
 * ExamAIFeedback}.
 */
@Entity
@Table(name = "exam_ai_feedbacks")
public class ExamAIFeedbackEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "exam_submission_id", nullable = false, unique = true)
  private UUID examSubmissionId;

  @Column(name = "extracted_text")
  private String extractedText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "step_analyses")
  private List<StepAnalysis> stepAnalyses;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "criteria_scores")
  private List<CriteriaScore> criteriaScores;

  @Column(name = "ai_score_percent")
  private float aiScorePercent;

  private String feedback;

  @Column(name = "handwriting_match_score")
  private float handwritingMatchScore;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  protected ExamAIFeedbackEntity() {}

  public ExamAIFeedbackEntity(
      UUID id,
      UUID schoolId,
      UUID examSubmissionId,
      String extractedText,
      List<StepAnalysis> stepAnalyses,
      List<CriteriaScore> criteriaScores,
      float aiScorePercent,
      String feedback,
      float handwritingMatchScore,
      LocalDateTime processedAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.examSubmissionId = examSubmissionId;
    this.extractedText = extractedText;
    this.stepAnalyses = stepAnalyses;
    this.criteriaScores = criteriaScores;
    this.aiScorePercent = aiScorePercent;
    this.feedback = feedback;
    this.handwritingMatchScore = handwritingMatchScore;
    this.processedAt = processedAt;
  }

  public static ExamAIFeedbackEntity fromDomain(ExamAIFeedback domain) {
    return new ExamAIFeedbackEntity(
        domain.id(),
        domain.schoolId(),
        domain.examSubmissionId(),
        domain.extractedText(),
        domain.stepAnalyses(),
        domain.criteriaScores(),
        domain.aiScorePercent(),
        domain.feedback(),
        domain.handwritingMatchScore(),
        domain.processedAt());
  }

  public ExamAIFeedback toDomain() {
    return new ExamAIFeedback(
        id,
        schoolId,
        examSubmissionId,
        extractedText,
        stepAnalyses,
        criteriaScores,
        aiScorePercent,
        feedback,
        handwritingMatchScore,
        processedAt);
  }

  public UUID getExamSubmissionId() {
    return examSubmissionId;
  }
}
