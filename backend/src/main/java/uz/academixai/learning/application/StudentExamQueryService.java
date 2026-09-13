package uz.academixai.learning.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Exam;
import uz.academixai.domain.ExamGrade;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.StudentExamQuery;
import uz.academixai.learning.application.port.out.ExamGradeStore;
import uz.academixai.learning.application.port.out.ExamStore;
import uz.academixai.learning.application.port.out.ExamSubmissionAssessmentLookup;
import uz.academixai.learning.application.port.out.ExamSubmissionStore;
import uz.academixai.learning.application.port.out.StudentClassLookup;
import uz.academixai.learning.application.port.out.SubjectNameLookup;

/** Student-facing exam result read model owned by Learning. */
@Service
public class StudentExamQueryService implements StudentExamQuery {

  private final ExamStore exams;
  private final ExamSubmissionStore submissions;
  private final ExamSubmissionAssessmentLookup assessments;
  private final ExamGradeStore grades;
  private final StudentClassLookup studentClasses;
  private final SubjectNameLookup subjects;

  public StudentExamQueryService(
      ExamStore exams,
      ExamSubmissionStore submissions,
      ExamSubmissionAssessmentLookup assessments,
      ExamGradeStore grades,
      StudentClassLookup studentClasses,
      SubjectNameLookup subjects) {
    this.exams = exams;
    this.submissions = submissions;
    this.assessments = assessments;
    this.grades = grades;
    this.studentClasses = studentClasses;
    this.subjects = subjects;
  }

  @Override
  public List<ExamListItem> list(UUID schoolId, UUID studentId) {
    UUID classId = requireStudentClassId(schoolId, studentId);
    return exams.findBySchoolIdAndClassId(schoolId, classId).stream()
        .map(exam -> toListItem(exam, studentId))
        .toList();
  }

  @Override
  public ExamDetail getDetail(UUID schoolId, UUID studentId, UUID examId) {
    requireStudentClassId(schoolId, studentId);
    Exam exam =
        exams
            .findByIdAndSchoolId(examId, schoolId)
            .orElseThrow(StudentExamQueryService::examNotFound);
    ExamSubmission submission =
        submissions
            .findLatestByExamIdAndStudentId(examId, studentId)
            .orElseThrow(StudentExamQueryService::studentResultNotFound);
    return new ExamDetail(
        exam,
        submission,
        assessments.feedback(submission.id()).orElse(null),
        grades.findBySubmissionId(submission.id()).orElse(null));
  }

  private ExamListItem toListItem(Exam exam, UUID studentId) {
    ExamGrade grade =
        submissions
            .findLatestByExamIdAndStudentId(exam.id(), studentId)
            .flatMap(submission -> grades.findBySubmissionId(submission.id()))
            .orElse(null);
    return new ExamListItem(exam, subjects.name(exam.subjectId()), grade);
  }

  private UUID requireStudentClassId(UUID schoolId, UUID studentId) {
    return studentClasses
        .classId(schoolId, studentId)
        .orElseThrow(StudentExamQueryService::studentAccessDenied);
  }

  private static ApiException examNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_EXAM_NOT_FOUND", "Imtihon topilmadi.", "ID ni tekshiring.");
  }

  private static ApiException studentResultNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_EXAM_NOT_FOUND",
        "Sizning ushbu imtihon uchun natijangiz topilmadi.",
        "O'qituvchi bilan bog'laning.");
  }

  private static ApiException studentAccessDenied() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        "ERR_ACCESS_DENIED",
        "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
        "O'quvchi profili topilmadi.");
  }
}
