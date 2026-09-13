package uz.academixai.intelligence.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.application.GradingCriteriaService;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.AiUsageLogEntity;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.intelligence.application.port.out.HomeworkAnalysisStore;
import uz.academixai.intelligence.domain.GradingCriterion;

/** JPA compatibility adapter over Learning's current homework-analysis tables. */
@Repository
public class JpaHomeworkAnalysisStore implements HomeworkAnalysisStore {

  private final HomeworkSubmissionRepository submissions;
  private final HomeworkAssignmentRepository assignments;
  private final SubjectRepository subjects;
  private final SchoolClassRepository classes;
  private final AIFeedbackRepository feedback;
  private final AiUsageLogRepository usage;
  private final GradingCriteriaService criteria;

  public JpaHomeworkAnalysisStore(
      HomeworkSubmissionRepository submissions,
      HomeworkAssignmentRepository assignments,
      SubjectRepository subjects,
      SchoolClassRepository classes,
      AIFeedbackRepository feedback,
      AiUsageLogRepository usage,
      GradingCriteriaService criteria) {
    this.submissions = submissions;
    this.assignments = assignments;
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
  public Optional<HomeworkSubmission> findSubmission(UUID submissionId) {
    return submissions.findById(submissionId).map(HomeworkSubmissionEntity::toDomain);
  }

  @Override
  public Optional<AssignmentContext> findAssignmentContext(UUID assignmentId) {
    return assignments.findById(assignmentId).map(this::contextFrom);
  }

  @Override
  public void saveFeedback(AIFeedback value) {
    feedback.save(AIFeedbackEntity.fromDomain(value));
  }

  @Override
  public void saveSubmission(HomeworkSubmission submission) {
    submissions.save(HomeworkSubmissionEntity.fromDomain(submission));
  }

  @Override
  public void recordAiUsage(UUID schoolId, AssignmentContext context) {
    usage.save(
        new AiUsageLogEntity(
            UUID.randomUUID(),
            schoolId,
            context.classId(),
            context.subjectId(),
            context.teacherId(),
            "HOMEWORK",
            LocalDateTime.now()));
  }

  private AssignmentContext contextFrom(HomeworkAssignmentEntity assignment) {
    String subjectName =
        subjects
            .findById(assignment.getSubjectId())
            .map(subject -> subject.getName())
            .orElse("Fan");
    Integer grade =
        classes
            .findById(assignment.getClassId())
            .map(schoolClass -> schoolClass.getGrade())
            .orElse(null);
    List<GradingCriterion> configured =
        criteria.getForGrading(assignment.getTeacherId(), assignment.getSubjectId()).stream()
            .map(item -> new GradingCriterion(item.name(), item.weightPercent()))
            .toList();
    return new AssignmentContext(
        assignment.getTeacherId(),
        assignment.getClassId(),
        assignment.getSubjectId(),
        subjectName,
        grade,
        configured);
  }
}
