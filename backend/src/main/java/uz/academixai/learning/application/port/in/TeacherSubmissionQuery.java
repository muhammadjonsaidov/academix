package uz.academixai.learning.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.shared.ai.GradingMode;

/** Published Learning queries for teacher-owned homework submissions. */
public interface TeacherSubmissionQuery {

  record SubmissionWithFeedback(
      HomeworkSubmission submission,
      String studentName,
      String assignmentTitle,
      AIFeedback feedback,
      Grade grade,
      /** Set on the detail view only; null in lists. */
      GradingMode gradingMode) {}

  List<SubmissionWithFeedback> list(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID classId, SubmissionStatus status);

  SubmissionWithFeedback get(UUID schoolId, UUID teacherId, UUID submissionId);

  record SubmissionImage(byte[] content, String contentType) {}

  /**
   * The photo the student uploaded, for the teacher who owns the assignment; empty for text work.
   */
  java.util.Optional<SubmissionImage> image(UUID schoolId, UUID teacherId, UUID submissionId);

  /** One row per assignment that has AI-checked work waiting for the teacher. */
  record ReviewQueueItem(
      UUID assignmentId,
      String assignmentTitle,
      int waiting,
      int flagged,
      java.time.LocalDateTime oldestSubmittedAt) {}

  /** Assignments with waiting work, those with flagged work first, then the longest-waiting. */
  List<ReviewQueueItem> reviewQueue(UUID schoolId, UUID teacherId);
}
