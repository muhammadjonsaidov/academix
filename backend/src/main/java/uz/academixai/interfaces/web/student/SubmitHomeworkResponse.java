package uz.academixai.interfaces.web.student;

import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** academix_tz.md §2.4 — POST /student/homework/{assignmentId}/submit response. */
public record SubmitHomeworkResponse(UUID submissionId, String status, String message) {

  public static SubmitHomeworkResponse from(HomeworkSubmission domain) {
    return new SubmitHomeworkResponse(
        domain.id(), domain.status().name(), "Vazifangiz qabul qilindi. AI tahlil qilmoqda...");
  }
}
