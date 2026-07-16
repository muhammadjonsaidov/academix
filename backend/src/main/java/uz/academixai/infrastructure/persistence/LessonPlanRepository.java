package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonPlanRepository extends JpaRepository<LessonPlanEntity, UUID> {

  List<LessonPlanEntity> findByTeacherIdOrderByLessonDateDesc(UUID teacherId);

  List<LessonPlanEntity> findByTeacherIdAndClassIdOrderByLessonDateDesc(
      UUID teacherId, UUID classId);

  List<LessonPlanEntity> findByTeacherIdAndSubjectIdOrderByLessonDateDesc(
      UUID teacherId, UUID subjectId);

  List<LessonPlanEntity> findByTeacherIdAndSubjectIdAndClassIdOrderByLessonDateDesc(
      UUID teacherId, UUID subjectId, UUID classId);

  Optional<LessonPlanEntity> findByIdAndTeacherId(UUID id, UUID teacherId);
}
