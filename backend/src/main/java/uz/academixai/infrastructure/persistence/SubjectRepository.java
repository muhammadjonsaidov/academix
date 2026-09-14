package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

  List<SubjectEntity> findBySchoolIdOrderByName(UUID schoolId);

  long countBySchoolId(UUID schoolId);

  Optional<SubjectEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  /**
   * In-use check for DELETE /admin/subjects/{id} — every FK to subjects is ON DELETE CASCADE, so
   * deletion must be refused while anything references the subject. Checks the three tables a
   * subject can be reached through directly by admins/teachers (assignments, homework, exams);
   * syllabus/lesson-plan/criteria rows can only exist downstream of one of these. Note: {@code
   * exams} is RLS-enabled — safe here because this only runs inside an admin request where
   * RlsTransactionFilter has already SET LOCAL the school context.
   */
  @Query(
      value =
          """
          SELECT EXISTS(SELECT 1 FROM class_subject_teachers WHERE subject_id = :subjectId)
              OR EXISTS(SELECT 1 FROM homework_assignments WHERE subject_id = :subjectId)
              OR EXISTS(SELECT 1 FROM exams WHERE subject_id = :subjectId)
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("subjectId") UUID subjectId);
}
