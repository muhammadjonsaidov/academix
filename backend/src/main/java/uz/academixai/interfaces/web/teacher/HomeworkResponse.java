package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;

public record HomeworkResponse(
    UUID id,
    UUID classId,
    UUID subjectId,
    String title,
    String description,
    AssignmentType type,
    LocalDateTime assignedAt,
    LocalDateTime deadlineAt,
    int maxScore,
    boolean isActive,
    String syllabusReference) {

  public static HomeworkResponse from(HomeworkAssignment domain) {
    return new HomeworkResponse(
        domain.id(),
        domain.classId(),
        domain.subjectId(),
        domain.title(),
        domain.description(),
        domain.type(),
        domain.assignedAt(),
        domain.deadlineAt(),
        domain.maxScore(),
        domain.isActive(),
        domain.syllabusReference());
  }
}
