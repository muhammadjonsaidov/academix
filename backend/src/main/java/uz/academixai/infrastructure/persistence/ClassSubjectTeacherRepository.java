package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSubjectTeacherRepository
    extends JpaRepository<ClassSubjectTeacherEntity, UUID> {

  boolean existsBySchoolIdAndClassIdAndSubjectIdAndTeacherId(
      UUID schoolId, UUID classId, UUID subjectId, UUID teacherId);

  List<ClassSubjectTeacherEntity> findBySchoolId(UUID schoolId);

  Optional<ClassSubjectTeacherEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  List<ClassSubjectTeacherEntity> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId);

  boolean existsBySchoolIdAndTeacherIdAndClassIdAndSubjectId(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);
}
