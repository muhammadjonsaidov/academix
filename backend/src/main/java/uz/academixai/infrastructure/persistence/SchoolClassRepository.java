package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, UUID> {

  List<SchoolClassEntity> findBySchoolIdOrderByGradeAscLetterAsc(UUID schoolId);

  Optional<SchoolClassEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  List<SchoolClassEntity> findBySchoolIdAndClassTeacherId(UUID schoolId, UUID classTeacherId);

  boolean existsBySchoolIdAndGradeAndLetter(UUID schoolId, int grade, String letter);

  // Bulk import rows carry a human-readable class name (e.g. "9-D"), not a UUID — see
  // BulkImportService.
  Optional<SchoolClassEntity> findBySchoolIdAndFullNameIgnoreCase(UUID schoolId, String fullName);

  // school_classes.student_count was defined and read everywhere (ClassResponse,
  // TeacherClassResponse, admin analytics classProgressList) since Sprint 1 but never actually
  // written by any service — confirmed by a real request during Sprint 10's admin analytics work
  // returning studentCount: 0 for classes with real enrolled students. Kept in sync here rather
  // than computed on read (COUNT(*) FROM student_profiles) to match the column's existing
  // "denormalized roster size" design and avoid an extra join on every class list/response.
  @Modifying
  @Query("UPDATE SchoolClassEntity c SET c.studentCount = c.studentCount + 1 WHERE c.id = :classId")
  void incrementStudentCount(@Param("classId") UUID classId);

  @Modifying
  @Query(
      "UPDATE SchoolClassEntity c SET c.studentCount = c.studentCount - 1 "
          + "WHERE c.id = :classId AND c.studentCount > 0")
  void decrementStudentCount(@Param("classId") UUID classId);
}
