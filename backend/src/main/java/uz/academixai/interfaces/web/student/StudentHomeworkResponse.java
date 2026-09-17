package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;

/** academix_tz.md §2.4 — GET /student/homework(/{assignmentId}). */
public record StudentHomeworkResponse(
    UUID assignmentId,
    String subject,
    String title,
    LocalDateTime deadlineAt,
    boolean isLate,
    MyTaskResponse myTask,
    String submissionStatus) {

  public record MyTaskResponse(String taskContent) {}

  public static StudentHomeworkResponse from(HomeworkItem item) {
    return new StudentHomeworkResponse(
        item.assignmentId(),
        item.subjectName(),
        item.title(),
        item.deadlineAt(),
        item.isLate(),
        item.myTaskContent() == null ? null : new MyTaskResponse(item.myTaskContent()),
        item.submissionStatus());
  }
}
