package uz.academixai.learning.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamGrade;
import uz.academixai.domain.ExamSubmission;

/** Teacher-owned exam paper upload, review and grading workflow. */
public interface ExamSubmissionWorkflow {

  record Image(byte[] content, String contentType, String originalFilename) {}

  record SubmissionWithFeedback(
      ExamSubmission submission, String studentName, ExamAIFeedback feedback, ExamGrade grade) {}

  int bulkUpload(
      UUID schoolId, UUID teacherId, UUID examId, List<Image> images, List<UUID> studentIds);

  List<SubmissionWithFeedback> list(UUID schoolId, UUID teacherId, UUID examId);

  ExamGrade grade(
      UUID schoolId,
      UUID teacherId,
      UUID examId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment);

  int approveAll(UUID schoolId, UUID teacherId, UUID examId);
}
