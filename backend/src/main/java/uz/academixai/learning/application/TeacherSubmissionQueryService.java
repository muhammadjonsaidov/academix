package uz.academixai.learning.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uz.academixai.learning.domain.ReviewQueueOrder;
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
    Map<UUID, String> titles;
    if (assignmentId != null) {
      HomeworkAssignment assignment = requireOwnedAssignment(schoolId, teacherId, assignmentId);
      titles = Map.of(assignment.id(), assignment.title());
      results = submissions.findByAssignmentId(assignmentId);
    } else {
      titles =
          assignments.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
              .filter(assignment -> classId == null || assignment.classId().equals(classId))
              .collect(Collectors.toMap(HomeworkAssignment::id, HomeworkAssignment::title));
      results =
          titles.isEmpty()
              ? List.of()
              : submissions.findByAssignmentIds(List.copyOf(titles.keySet()));
    }
    List<SubmissionWithFeedback> items =
        results.stream()
            .filter(submission -> status == null || submission.status() == status)
            .map(submission -> withFeedback(submission, titles.get(submission.assignmentId())))
            .toList();
    return items.stream().sorted(order(status)).toList();
  }

  @Override
  public SubmissionWithFeedback get(UUID schoolId, UUID teacherId, UUID submissionId) {
    HomeworkSubmission submission =
        submissions
            .findByIdAndSchoolId(submissionId, schoolId)
            .orElseThrow(TeacherSubmissionQueryService::notFound);
    HomeworkAssignment assignment =
        requireOwnedAssignment(schoolId, teacherId, submission.assignmentId());
    return withFeedback(submission, assignment.title());
  }

  /**
   * The review queue (work the AI finished, waiting for the teacher) puts flagged work first, then
   * the oldest; every other view lists the newest first.
   */
  private static Comparator<SubmissionWithFeedback> order(SubmissionStatus status) {
    Comparator<SubmissionWithFeedback> bySubmitted =
        Comparator.comparing(item -> item.submission().submittedAt());
    if (status == SubmissionStatus.AI_DONE) {
      Comparator<SubmissionWithFeedback> flaggedFirst =
          Comparator.comparing(item -> !ReviewQueueOrder.needsCloseLook(item.feedback()));
      return flaggedFirst.thenComparing(bySubmitted);
    }
    return bySubmitted.reversed();
  }

  private SubmissionWithFeedback withFeedback(HomeworkSubmission submission, String title) {
    return new SubmissionWithFeedback(
        submission,
        names.fullName(submission.studentId()),
        title,
        assessments.feedback(submission.id()).orElse(null),
        assessments.grade(submission.id()).orElse(null));
  }

  private HomeworkAssignment requireOwnedAssignment(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
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
    return assignment;
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Topshiriq topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }
}
