package uz.academixai.learning.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What Learning tells students and parents about; delivery (inbox, Telegram) is not its concern.
 */
public interface LearningNotifier {

  /** A new assignment was given to every student of the class. */
  void homeworkAssigned(
      UUID schoolId, UUID classId, UUID assignmentId, String title, LocalDateTime deadlineAt);

  /** The teacher confirmed a grade: the student and the student's parents hear about it. */
  void homeworkGraded(
      UUID studentId,
      UUID submissionId,
      String assignmentTitle,
      int score,
      int fivePointGrade,
      boolean regraded);

  /** The deadline is close and the student has not submitted yet. */
  void deadlineApproaching(
      UUID studentId, UUID assignmentId, String title, LocalDateTime deadlineAt);
}
