package uz.academixai.learning.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.learning.application.port.in.TeacherSubmissionQuery;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;
import uz.academixai.learning.application.port.out.StudentNameLookup;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;
import uz.academixai.shared.error.ApiException;

/** Teacher-scoped submission read model owned by Learning. */
@Service
public class TeacherSubmissionQueryService implements TeacherSubmissionQuery {

  private final HomeworkAssignmentStore assignments;
  private final HomeworkSubmissionReadRepository submissions;
  private final StudentNameLookup names;
  private final SubmissionAssessmentLookup assessments;

  public TeacherSubmissionQueryService(
      HomeworkAssignmentStore assignments,
      HomeworkSubmissionReadRepository submissions,
      StudentNameLookup names,
      SubmissionAssessmentLookup assessments) {
    this.assignments = assignments;
    this.submissions = submissions;
    this.names = names;
    this.assessments = assessments;
  }

  @Override
  public List<SubmissionWithFeedback> list(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID classId, SubmissionStatus status) {
    List<HomeworkSubmission> results;
    if (assignmentId != null) {
      requireOwnedAssignment(schoolId, teacherId, assignmentId);
      results = submissions.findByAssignmentId(assignmentId);
    } else {
      List<UUID> assignmentIds =
          assignments.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
              .filter(assignment -> classId == null || assignment.classId().equals(classId))
              .map(HomeworkAssignment::id)
              .toList();
      results =
          assignmentIds.isEmpty() ? List.of() : submissions.findByAssignmentIds(assignmentIds);
    }
    return results.stream()
        .filter(submission -> status == null || submission.status() == status)
        .map(this::withFeedback)
        .toList();
  }

  @Override
  public SubmissionWithFeedback get(UUID schoolId, UUID teacherId, UUID submissionId) {
    HomeworkSubmission submission =
        submissions
            .findByIdAndSchoolId(submissionId, schoolId)
            .orElseThrow(TeacherSubmissionQueryService::notFound);
    requireOwnedAssignment(schoolId, teacherId, submission.assignmentId());
    return withFeedback(submission);
  }

  private SubmissionWithFeedback withFeedback(HomeworkSubmission submission) {
    return new SubmissionWithFeedback(
        submission,
        names.fullName(submission.studentId()),
        assessments.feedback(submission.id()).orElse(null),
        assessments.grade(submission.id()).orElse(null));
  }

  private void requireOwnedAssignment(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(TeacherSubmissionQueryService::notFound);
    if (!assignment.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi ko'rishi mumkin.");
    }
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Topshiriq topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }
}
