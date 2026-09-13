package uz.academixai.learning.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;

/** Published Learning queries for teacher-owned homework submissions. */
public interface TeacherSubmissionQuery {

  record SubmissionWithFeedback(
      HomeworkSubmission submission, String studentName, AIFeedback feedback, Grade grade) {}

  List<SubmissionWithFeedback> list(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID classId, SubmissionStatus status);

  SubmissionWithFeedback get(UUID schoolId, UUID teacherId, UUID submissionId);
}
