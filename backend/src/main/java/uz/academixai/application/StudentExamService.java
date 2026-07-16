package uz.academixai.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Exam;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackRepository;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamGradeEntity;
import uz.academixai.infrastructure.persistence.ExamGradeRepository;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * Gap-fill deviation, judgment call (see CLAUDE.md): academix_tz.md never documents a
 * student-facing exam-results endpoint anywhere — mirrors {@link StudentSubmissionService}'s {@code
 * listSubmissions}/{@code getSubmissionDetail} shape exactly. Handwriting match score is
 * deliberately never exposed to the student (same rule already applied to homework's {@code GET
 * /student/submissions/{id}} — "plagiarism va handwriting score ko'RINMAYDI").
 */
@Service
public class StudentExamService {

  private final ExamRepository examRepository;
  private final ExamSubmissionRepository submissionRepository;
  private final ExamAIFeedbackRepository feedbackRepository;
  private final ExamGradeRepository gradeRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SubjectRepository subjectRepository;

  public StudentExamService(
      ExamRepository examRepository,
      ExamSubmissionRepository submissionRepository,
      ExamAIFeedbackRepository feedbackRepository,
      ExamGradeRepository gradeRepository,
      StudentProfileRepository studentProfileRepository,
      SubjectRepository subjectRepository) {
    this.examRepository = examRepository;
    this.submissionRepository = submissionRepository;
    this.feedbackRepository = feedbackRepository;
    this.gradeRepository = gradeRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.subjectRepository = subjectRepository;
  }

  public record ExamListItem(Exam exam, String subjectName, ExamGradeEntity grade) {}

  public List<ExamListItem> list(UUID schoolId, UUID studentId) {
    UUID classId = requireStudentClassId(schoolId, studentId);
    return examRepository.findBySchoolIdAndClassIdOrderByExamDateDesc(schoolId, classId).stream()
        .map(entity -> toListItem(entity, studentId))
        .toList();
  }

  private ExamListItem toListItem(ExamEntity entity, UUID studentId) {
    Exam exam = entity.toDomain();
    String subjectName =
        subjectRepository.findById(exam.subjectId()).map(SubjectEntity::getName).orElse("Fan");
    ExamGradeEntity grade =
        submissionRepository
            .findFirstByExamIdAndStudentIdOrderByUploadedAtDesc(exam.id(), studentId)
            .flatMap(sub -> gradeRepository.findByExamSubmissionId(sub.getId()))
            .orElse(null);
    return new ExamListItem(exam, subjectName, grade);
  }

  public record ExamDetail(
      Exam exam,
      ExamSubmissionEntity submission,
      ExamAIFeedbackEntity feedback,
      ExamGradeEntity grade) {}

  public ExamDetail getDetail(UUID schoolId, UUID studentId, UUID examId) {
    requireStudentClassId(schoolId, studentId);
    ExamEntity exam =
        examRepository
            .findByIdAndSchoolId(examId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_EXAM_NOT_FOUND",
                        "Imtihon topilmadi.",
                        "ID ni tekshiring."));
    ExamSubmissionEntity submission =
        submissionRepository
            .findFirstByExamIdAndStudentIdOrderByUploadedAtDesc(examId, studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_EXAM_NOT_FOUND",
                        "Sizning ushbu imtihon uchun natijangiz topilmadi.",
                        "O'qituvchi bilan bog'laning."));
    return new ExamDetail(
        exam.toDomain(),
        submission,
        feedbackRepository.findByExamSubmissionId(submission.getId()).orElse(null),
        gradeRepository.findByExamSubmissionId(submission.getId()).orElse(null));
  }

  private UUID requireStudentClassId(UUID schoolId, UUID studentId) {
    return studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::toDomain)
        .filter(p -> schoolId.equals(p.schoolId()))
        .map(p -> p.classId())
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ERR_ACCESS_DENIED",
                    "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                    "O'quvchi profili topilmadi."));
  }
}
