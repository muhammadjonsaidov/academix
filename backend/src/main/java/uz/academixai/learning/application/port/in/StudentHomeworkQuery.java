package uz.academixai.learning.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkSubmission;

/** Published Learning read queries for student homework and submission history. */
public interface StudentHomeworkQuery {

  record HomeworkItem(
      UUID assignmentId,
      String subjectName,
      String title,
      LocalDateTime deadlineAt,
      boolean isLate,
      String submissionStatus,
      String myTaskContent) {}

  record SubmissionDetail(HomeworkSubmission submission, AIFeedback feedback, Grade grade) {}

  List<HomeworkItem> listHomework(UUID schoolId, UUID studentId);

  HomeworkItem getHomeworkDetail(UUID schoolId, UUID studentId, UUID assignmentId);

  List<HomeworkSubmission> listSubmissions(UUID schoolId, UUID studentId);

  SubmissionDetail getSubmissionDetail(UUID schoolId, UUID studentId, UUID submissionId);
}
