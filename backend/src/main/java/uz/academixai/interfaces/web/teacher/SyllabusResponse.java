package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.domain.FileType;
import uz.academixai.learning.domain.SyllabusProcessingStatus;
import uz.academixai.learning.domain.TeacherSyllabus;

public record SyllabusResponse(
    UUID id,
    UUID subjectId,
    UUID classId,
    String title,
    String fileUrl,
    FileType fileType,
    boolean isProcessed,
    SyllabusProcessingStatus processingStatus,
    String processingError,
    LocalDateTime processedAt,
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
        syllabus.processingStatus(),
        syllabus.processingError(),
        syllabus.processedAt(),
        syllabus.uploadedAt());
  }
}
