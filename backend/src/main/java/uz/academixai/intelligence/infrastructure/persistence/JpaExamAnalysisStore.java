package uz.academixai.intelligence.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.AiUsageLogEntity;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackRepository;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.intelligence.application.port.out.ExamAnalysisStore;
import uz.academixai.intelligence.domain.GradingCriterion;
import uz.academixai.learning.application.GradingCriteriaService;

/** JPA compatibility adapter over Learning's current exam-analysis tables. */
@Repository
public class JpaExamAnalysisStore implements ExamAnalysisStore {

  private final ExamSubmissionRepository submissions;
  private final ExamRepository exams;
  private final SubjectRepository subjects;
  private final SchoolClassRepository classes;
  private final ExamAIFeedbackRepository feedback;
  private final AiUsageLogRepository usage;
  private final GradingCriteriaService criteria;

  public JpaExamAnalysisStore(
      ExamSubmissionRepository submissions,
      ExamRepository exams,
      SubjectRepository subjects,
      SchoolClassRepository classes,
      ExamAIFeedbackRepository feedback,
      AiUsageLogRepository usage,
      GradingCriteriaService criteria) {
    this.submissions = submissions;
    this.exams = exams;
    this.subjects = subjects;
    this.classes = classes;
    this.feedback = feedback;
    this.usage = usage;
    this.criteria = criteria;
  }

  @Override
  public boolean claimForAnalysis(UUID submissionId) {
    return submissions.claimForAi(
            submissionId, SubmissionStatus.SUBMITTED, SubmissionStatus.AI_PROCESSING)
        == 1;
  }

  @Override
  public Optional<ExamSubmission> findSubmission(UUID submissionId) {
    return submissions.findById(submissionId).map(ExamSubmissionEntity::toDomain);
  }

  @Override
  public Optional<ExamContext> findExamContext(UUID examId) {
    return exams.findById(examId).map(this::contextFrom);
  }

  @Override
  public void saveFeedback(ExamAIFeedback value) {
    feedback.save(ExamAIFeedbackEntity.fromDomain(value));
  }

  @Override
  public void saveSubmission(ExamSubmission submission) {
    submissions.save(ExamSubmissionEntity.fromDomain(submission));
  }

  @Override
  public void recordAiUsage(UUID schoolId, ExamContext context) {
    usage.save(
        new AiUsageLogEntity(
            UUID.randomUUID(),
            schoolId,
            context.classId(),
            context.subjectId(),
            context.teacherId(),
            "EXAM",
            LocalDateTime.now()));
  }

  private ExamContext contextFrom(ExamEntity exam) {
    String subjectName =
        subjects
            .findById(exam.toDomain().subjectId())
            .map(subject -> subject.getName())
            .orElse("Fan");
    Integer grade =
        classes.findById(exam.getClassId()).map(schoolClass -> schoolClass.getGrade()).orElse(null);
    List<GradingCriterion> configured =
        criteria.getForGrading(exam.getTeacherId(), exam.toDomain().subjectId()).stream()
            .map(item -> new GradingCriterion(item.name(), item.weightPercent()))
            .toList();
    return new ExamContext(
        exam.getTeacherId(),
        exam.getClassId(),
        exam.toDomain().subjectId(),
        subjectName,
        grade,
        configured);
  }
}
