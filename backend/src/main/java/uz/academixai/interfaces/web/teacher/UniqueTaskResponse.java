package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.learning.application.port.in.UniqueTaskReview.TaskWithStudent;

public record UniqueTaskResponse(
    UUID taskId,
    UUID studentId,
    String studentName,
    String taskContent,
    boolean isApproved,
    boolean flaggedForReview,
    boolean fallbackToStandard) {

  public static UniqueTaskResponse from(TaskWithStudent item) {
    var task = item.task();
    return new UniqueTaskResponse(
        task.id(),
        task.studentId(),
        item.studentName(),
        task.taskContent(),
        task.teacherApproved(),
        task.flaggedForReview(),
        task.fallbackToStandard());
  }
}
