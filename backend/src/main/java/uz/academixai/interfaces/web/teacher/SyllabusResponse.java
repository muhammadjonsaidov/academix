package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.FileType;
import uz.academixai.domain.TeacherSyllabus;

public record SyllabusResponse(
    UUID id,
    UUID subjectId,
    UUID classId,
    String title,
    String fileUrl,
    FileType fileType,
    boolean isProcessed,
    LocalDateTime uploadedAt) {

  public static SyllabusResponse from(TeacherSyllabus syllabus) {
    return new SyllabusResponse(
        syllabus.id(),
        syllabus.subjectId(),
        syllabus.classId(),
        syllabus.title(),
        syllabus.fileUrl(),
        syllabus.fileType(),
        syllabus.isProcessed(),
        syllabus.uploadedAt());
  }
}
