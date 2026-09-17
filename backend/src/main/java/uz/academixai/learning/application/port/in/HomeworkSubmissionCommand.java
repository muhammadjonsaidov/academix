package uz.academixai.learning.application.port.in;

import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionType;

/** Published Learning command for accepting a student's homework submission. */
public interface HomeworkSubmissionCommand {

  record Image(byte[] content, String contentType) {}

  HomeworkSubmission submit(
      UUID schoolId,
      UUID studentId,
      UUID assignmentId,
      SubmissionType type,
      String textContent,
      Image image);
}
