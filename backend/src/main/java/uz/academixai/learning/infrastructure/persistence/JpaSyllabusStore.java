package uz.academixai.learning.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.TeacherSyllabusEntity;
import uz.academixai.infrastructure.persistence.TeacherSyllabusRepository;
import uz.academixai.learning.application.port.out.SyllabusStore;
import uz.academixai.learning.domain.TeacherSyllabus;

/**
 * Compatibility adapter for {@code teacher_syllabuses}. The JPA entity and repository still live in
 * the legacy persistence package, so this class is the seam; it keeps the state-transition methods
 * on the entity where they are and exposes only the port's operations.
 */
@Repository
public class JpaSyllabusStore implements SyllabusStore {

  private final TeacherSyllabusRepository syllabuses;

  public JpaSyllabusStore(TeacherSyllabusRepository syllabuses) {
    this.syllabuses = syllabuses;
  }

  @Override
  public TeacherSyllabus save(TeacherSyllabus syllabus) {
    return syllabuses.save(TeacherSyllabusEntity.fromDomain(syllabus)).toDomain();
  }

  @Override
  public Optional<TeacherSyllabus> findOwned(UUID teacherId, UUID syllabusId) {
    return syllabuses
        .findByIdAndTeacherId(syllabusId, teacherId)
        .map(TeacherSyllabusEntity::toDomain);
  }

  @Override
  public List<TeacherSyllabus> list(UUID teacherId, UUID subjectId, UUID classId) {
    var entities =
        switch ((subjectId != null ? 1 : 0) + (classId != null ? 2 : 0)) {
          case 3 ->
              syllabuses.findByTeacherIdAndSubjectIdAndClassIdOrderByUploadedAtDesc(
                  teacherId, subjectId, classId);
          case 2 -> syllabuses.findByTeacherIdAndClassIdOrderByUploadedAtDesc(teacherId, classId);
          case 1 ->
              syllabuses.findByTeacherIdAndSubjectIdOrderByUploadedAtDesc(teacherId, subjectId);
          default -> syllabuses.findByTeacherIdOrderByUploadedAtDesc(teacherId);
        };
    return entities.stream().map(TeacherSyllabusEntity::toDomain).toList();
  }

  @Override
  public Optional<Source> startProcessing(UUID syllabusId) {
    return syllabuses
        .findById(syllabusId)
        .map(
            syllabus -> {
              syllabus.markProcessing();
              return toSource(syllabus);
            });
  }

  @Override
  public Optional<Source> sourceOf(UUID syllabusId) {
    return syllabuses.findById(syllabusId).map(JpaSyllabusStore::toSource);
  }

  @Override
  public void markReady(UUID syllabusId, String extractedText) {
    syllabuses
        .findById(syllabusId)
        .ifPresent(syllabus -> syllabus.markReady(extractedText, LocalDateTime.now()));
  }

  private static Source toSource(TeacherSyllabusEntity syllabus) {
    return new Source(
        syllabus.getId(),
        syllabus.getTeacherId(),
        syllabus.getSubjectId(),
        syllabus.getClassId(),
        syllabus.getFileUrl(),
        syllabus.getFileType());
  }

  @Override
  public void markFailed(UUID syllabusId, String processingError) {
    syllabuses.findById(syllabusId).ifPresent(syllabus -> syllabus.markFailed(processingError));
  }
}
