package uz.academixai.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Exam;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.ai.AiBudgetService;
import uz.academixai.infrastructure.ai.AiCallCategory;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Nazorat ishi (imtihon)" — teacher CRUD over {@code exams}, plus the
 * pre-flight AI-cost estimate the spec requires on creation (§8, "teacher isn't surprised mid-way
 * through grading 30 papers").
 */
@Service
public class ExamService {

  // §2.3: "warning?: Bu imtihon zaxiraning ko'p qismini sarflaydi" fires once the estimate
  // crosses 70% of what's left in the protected exam reserve.
  private static final double WARNING_THRESHOLD = 0.7;

  private final ExamRepository examRepository;
  private final ExamSubmissionRepository examSubmissionRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final TeacherContextService teacherContextService;
  private final AiBudgetService aiBudgetService;

  public ExamService(
      ExamRepository examRepository,
      ExamSubmissionRepository examSubmissionRepository,
      StudentProfileRepository studentProfileRepository,
      TeacherContextService teacherContextService,
      AiBudgetService aiBudgetService) {
    this.examRepository = examRepository;
    this.examSubmissionRepository = examSubmissionRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.teacherContextService = teacherContextService;
    this.aiBudgetService = aiBudgetService;
  }

  public record CreateResult(
      Exam exam, int estimatedAiCalls, int remainingExamBudget, String warning) {}

  public CreateResult create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      LocalDate examDate,
      int maxScore) {
    teacherContextService.requireAssignedToClassAndSubject(schoolId, teacherId, classId, subjectId);

    int estimatedAiCalls = studentProfileRepository.countByClassIdAndSchoolId(classId, schoolId);
    int remainingExamBudget = aiBudgetService.remainingBudget(schoolId, AiCallCategory.EXAM);
    String warning =
        estimatedAiCalls > remainingExamBudget * WARNING_THRESHOLD
            ? "Bu imtihon zaxiraning ko'p qismini sarflaydi"
            : null;

    Exam exam =
        new Exam(
            UUID.randomUUID(),
            schoolId,
            classId,
            subjectId,
            teacherId,
            title,
            examDate,
            maxScore,
            LocalDateTime.now());
    Exam saved = examRepository.save(ExamEntity.fromDomain(exam)).toDomain();
    return new CreateResult(saved, estimatedAiCalls, remainingExamBudget, warning);
  }

  public List<Exam> list(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    List<ExamEntity> all =
        examRepository.findBySchoolIdAndTeacherIdOrderByExamDateDesc(schoolId, teacherId);
    return all.stream()
        .filter(e -> classId == null || e.getClassId().equals(classId))
        .filter(e -> subjectId == null || e.toDomain().subjectId().equals(subjectId))
        .map(ExamEntity::toDomain)
        .toList();
  }

  public record ExamSummary(Exam exam, int submissionsCount, int gradedCount) {}

  public List<ExamSummary> listWithCounts(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    return list(schoolId, teacherId, classId, subjectId).stream()
        .map(
            exam ->
                new ExamSummary(
                    exam,
                    examSubmissionRepository.countByExamId(exam.id()),
                    examSubmissionRepository.countByExamIdAndStatus(
                        exam.id(), SubmissionStatus.GRADED)))
        .toList();
  }

  public Exam get(UUID schoolId, UUID examId) {
    return requireOwned(schoolId, examId).toDomain();
  }

  ExamEntity requireOwned(UUID schoolId, UUID examId) {
    return examRepository
        .findByIdAndSchoolId(examId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_EXAM_NOT_FOUND",
                    "Imtihon topilmadi.",
                    "ID ni tekshiring."));
  }
}
