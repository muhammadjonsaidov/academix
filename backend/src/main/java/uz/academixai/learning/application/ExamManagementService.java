package uz.academixai.learning.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Exam;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.ExamManagement;
import uz.academixai.learning.application.port.out.ClassSizeQuery;
import uz.academixai.learning.application.port.out.ExamAiBudget;
import uz.academixai.learning.application.port.out.ExamStore;
import uz.academixai.learning.application.port.out.ExamSubmissionStatistics;
import uz.academixai.school.application.port.in.TeacherAccess;

/** Teacher exam lifecycle use cases owned by the Learning context. */
@Service
public class ExamManagementService implements ExamManagement {

  private static final double WARNING_THRESHOLD = 0.7;

  private final ExamStore exams;
  private final ExamSubmissionStatistics submissions;
  private final ClassSizeQuery classSize;
  private final TeacherAccess teacherAccess;
  private final ExamAiBudget aiBudget;

  public ExamManagementService(
      ExamStore exams,
      ExamSubmissionStatistics submissions,
      ClassSizeQuery classSize,
      TeacherAccess teacherAccess,
      ExamAiBudget aiBudget) {
    this.exams = exams;
    this.submissions = submissions;
    this.classSize = classSize;
    this.teacherAccess = teacherAccess;
    this.aiBudget = aiBudget;
  }

  @Override
  public CreateResult create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      LocalDate examDate,
      int maxScore) {
    teacherAccess.requireAssignedToClassAndSubject(schoolId, teacherId, classId, subjectId);
    int estimatedAiCalls = classSize.activeStudentCount(schoolId, classId);
    int remainingExamBudget = aiBudget.remainingCalls(schoolId);
    String warning =
        estimatedAiCalls > remainingExamBudget * WARNING_THRESHOLD
            ? "Bu imtihon zaxiraning ko'p qismini sarflaydi"
            : null;
    Exam saved =
        exams.save(
            new Exam(
                UUID.randomUUID(),
                schoolId,
                classId,
                subjectId,
                teacherId,
                title,
                examDate,
                maxScore,
                LocalDateTime.now()));
    return new CreateResult(saved, estimatedAiCalls, remainingExamBudget, warning);
  }

  @Override
  public List<ExamSummary> listWithCounts(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    return exams.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
        .filter(exam -> classId == null || exam.classId().equals(classId))
        .filter(exam -> subjectId == null || exam.subjectId().equals(subjectId))
        .map(
            exam ->
                new ExamSummary(
                    exam, submissions.countAll(exam.id()), submissions.countGraded(exam.id())))
        .toList();
  }

  @Override
  public Exam get(UUID schoolId, UUID examId) {
    return exams
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
