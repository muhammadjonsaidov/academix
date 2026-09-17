package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.learning.domain.FileType;
import uz.academixai.learning.domain.TeacherSyllabus;

/**
 * Persistence port for {@code teacher_syllabuses} — Learning owns an uploaded syllabus, including
 * its processing state.
 */
public interface SyllabusStore {

  TeacherSyllabus save(TeacherSyllabus syllabus);

  Optional<TeacherSyllabus> findOwned(UUID teacherId, UUID syllabusId);

  List<TeacherSyllabus> list(UUID teacherId, UUID subjectId, UUID classId);

  /**
   * Marks the syllabus PROCESSING and returns what ingestion needs — the file plus the
   * subject/class scope its chunks must carry, which the caller cannot know before the row is read.
   */
  Optional<Source> startProcessing(UUID syllabusId);

  /** Read-only view of the same facts, for callers that must not change processing state. */
  Optional<Source> sourceOf(UUID syllabusId);

  void markReady(UUID syllabusId, String extractedText);

  void markFailed(UUID syllabusId, String processingError);

  record Source(
      UUID id, UUID teacherId, UUID subjectId, UUID classId, String objectKey, FileType fileType) {}
}
