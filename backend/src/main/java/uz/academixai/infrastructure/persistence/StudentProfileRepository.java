package uz.academixai.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, UUID> {

  Optional<StudentProfileEntity> findByUserId(UUID userId);

  // {studentId} path params (academix_tz.md §2.2) identify the student's User id, not the
  // internal student_profiles surrogate id — matches what GET /admin/students returns.
  Optional<StudentProfileEntity> findByUserIdAndSchoolId(UUID userId, UUID schoolId);

  List<StudentProfileEntity> findByClassIdAndSchoolId(UUID classId, UUID schoolId);

  List<StudentProfileEntity> findBySchoolId(UUID schoolId);

  int countByClassIdAndSchoolId(UUID classId, UUID schoolId);

  int countBySchoolIdAndIsActiveTrue(UUID schoolId);

  /**
   * Joined view for GET /admin/students — no @ManyToOne relation is mapped (project convention; see
   * SchoolEntity/UserEntity), so the join to {@code users} is done here directly.
   */
  interface StudentListRow {
    UUID getUserId();

    String getFirstName();

    String getLastName();

    String getPhone();

    UUID getClassId();

    String getStudentNumber();

    LocalDate getBirthDate();

    boolean getIsActive();
  }

  @Query(
      value =
          """
          SELECT u.id AS userId, u.first_name AS firstName, u.last_name AS lastName,
                 u.phone AS phone, sp.class_id AS classId, sp.student_number AS studentNumber,
                 sp.birth_date AS birthDate, sp.is_active AS isActive
          FROM student_profiles sp
          JOIN users u ON u.id = sp.user_id
          WHERE sp.school_id = :schoolId
            AND u.role = 'STUDENT'
            AND (:classId IS NULL OR sp.class_id = :classId)
            AND (:search IS NULL
                 OR u.first_name ILIKE CONCAT('%', :search, '%')
                 OR u.last_name ILIKE CONCAT('%', :search, '%'))
          ORDER BY u.last_name, u.first_name
          """,
      nativeQuery = true)
  List<StudentListRow> searchBySchool(
      @Param("schoolId") UUID schoolId,
      @Param("classId") UUID classId,
      @Param("search") String search);
}
