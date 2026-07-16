package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherSyllabusRepository extends JpaRepository<TeacherSyllabusEntity, UUID> {

  List<TeacherSyllabusEntity> findByTeacherIdOrderByUploadedAtDesc(UUID teacherId);

  List<TeacherSyllabusEntity> findByTeacherIdAndSubjectIdOrderByUploadedAtDesc(
      UUID teacherId, UUID subjectId);

  List<TeacherSyllabusEntity> findByTeacherIdAndClassIdOrderByUploadedAtDesc(
      UUID teacherId, UUID classId);

  List<TeacherSyllabusEntity> findByTeacherIdAndSubjectIdAndClassIdOrderByUploadedAtDesc(
      UUID teacherId, UUID subjectId, UUID classId);

  Optional<TeacherSyllabusEntity> findByIdAndTeacherId(UUID id, UUID teacherId);
}
