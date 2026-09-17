package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;
import uz.academixai.learning.application.port.out.StudentClassLookup;
import uz.academixai.learning.application.port.out.StudentUniqueTaskStore;
import uz.academixai.learning.application.port.out.SubjectNameLookup;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;
import uz.academixai.shared.error.ApiException;

/** Student homework and submission-history read use cases owned by Learning. */
@Service
public class StudentHomeworkQueryService implements StudentHomeworkQuery {

  private final HomeworkAssignmentStore assignments;
  private final HomeworkSubmissionReadRepository submissions;
  private final StudentClassLookup studentClasses;
  private final StudentUniqueTaskStore uniqueTasks;
  private final SubjectNameLookup subjects;
  private final SubmissionAssessmentLookup assessments;

  public StudentHomeworkQueryService(
      HomeworkAssignmentStore assignments,
      HomeworkSubmissionReadRepository submissions,
      StudentClassLookup studentClasses,
      StudentUniqueTaskStore uniqueTasks,
      SubjectNameLookup subjects,
      SubmissionAssessmentLookup assessments) {
    this.assignments = assignments;
    this.submissions = submissions;
    this.studentClasses = studentClasses;
    this.uniqueTasks = uniqueTasks;
    this.subjects = subjects;
    this.assessments = assessments;
  }

  @Override
  public List<HomeworkItem> listHomework(UUID schoolId, UUID studentId) {
    UUID classId = requireStudentClass(schoolId, studentId);
    return assignments.findBySchoolIdAndClassId(schoolId, classId).stream()
        .filter(HomeworkAssignment::tasksPublished)
        .map(assignment -> itemFor(assignment, studentId))
        .toList();
  }

  @Override
  public HomeworkItem getHomeworkDetail(UUID schoolId, UUID studentId, UUID assignmentId) {
    HomeworkAssignment assignment = requirePublishedAssignment(schoolId, assignmentId);
    if (!assignment.classId().equals(requireStudentClass(schoolId, studentId))) {
      throw studentAccessDenied();
    }
    return itemFor(assignment, studentId);
  }

  @Override
  public List<HomeworkSubmission> listSubmissions(UUID schoolId, UUID studentId) {
    return submissions.findByStudentId(studentId).stream()
        .filter(submission -> schoolId.equals(submission.schoolId()))
        .toList();
  }

  @Override
  public SubmissionDetail getSubmissionDetail(UUID schoolId, UUID studentId, UUID submissionId) {
    HomeworkSubmission submission =
        submissions
            .findByIdAndSchoolId(submissionId, schoolId)
            .orElseThrow(StudentHomeworkQueryService::homeworkNotFound);
    if (!submission.studentId().equals(studentId)) {
      throw studentAccessDenied();
    }
    return new SubmissionDetail(
        submission,
        assessments.feedback(submissionId).orElse(null),
        assessments.grade(submissionId).orElse(null));
  }

  private HomeworkItem itemFor(HomeworkAssignment assignment, UUID studentId) {
    HomeworkSubmission existing =
        submissions.findByAssignmentIdAndStudentId(assignment.id(), studentId).orElse(null);
    String taskContent =
        assignment.type() == AssignmentType.UNIQUE_GENERATED
            ? uniqueTasks
                .findByAssignmentIdAndStudentId(assignment.id(), studentId)
                .map(task -> task.taskContent())
                .orElse(null)
            : null;
    return new HomeworkItem(
        assignment.id(),
        subjects.name(assignment.subjectId()),
        assignment.title(),
        assignment.deadlineAt(),
        existing == null ? LocalDateTime.now().isAfter(assignment.deadlineAt()) : existing.isLate(),
        existing == null
            ? "PENDING"
            : existing.status() == SubmissionStatus.GRADED ? "GRADED" : "SUBMITTED",
        taskContent);
  }

  private HomeworkAssignment requirePublishedAssignment(UUID schoolId, UUID assignmentId) {
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(StudentHomeworkQueryService::homeworkNotFound);
    if (!assignment.tasksPublished()) {
      throw homeworkNotFound();
    }
    return assignment;
  }

  private UUID requireStudentClass(UUID schoolId, UUID studentId) {
    return studentClasses
        .classId(schoolId, studentId)
        .orElseThrow(StudentHomeworkQueryService::studentAccessDenied);
  }

  private static ApiException homeworkNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Uy vazifasi topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }

  private static ApiException studentAccessDenied() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        "ERR_ACCESS_DENIED",
        "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
        "O'quvchi profili topilmadi yoki vazifa sizning sinfingizga tegishli emas.");
  }
}
