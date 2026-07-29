package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLinkEntity, UUID> {

  List<ParentStudentLinkEntity> findByParentUserIdAndIsActiveTrue(UUID parentUserId);

  List<ParentStudentLinkEntity> findByStudentUserIdAndIsActiveTrue(UUID studentUserId);

  Optional<ParentStudentLinkEntity> findByParentUserIdAndStudentUserId(
      UUID parentUserId, UUID studentUserId);

  boolean existsByParentUserIdAndStudentUserIdAndIsActiveTrue(
      UUID parentUserId, UUID studentUserId);

  /**
   * Joined child view for the admin parents list/check (GET /admin/parents) — links carry no
   * school_id by design (V34), so school scoping goes through the linked student's profile, the
   * same join {@code SchoolContextResolver} uses.
   */
  interface ChildRow {
    UUID getParentUserId();

    UUID getStudentUserId();

    String getRelation();

    String getFirstName();

    String getLastName();

    String getClassName();
  }

  @Query(
      value =
          """
          SELECT l.parent_user_id AS parentUserId, l.student_user_id AS studentUserId,
                 l.relation AS relation, u.first_name AS firstName, u.last_name AS lastName,
                 sc.full_name AS className
          FROM parent_student_links l
          JOIN users u ON u.id = l.student_user_id
          JOIN student_profiles sp ON sp.user_id = l.student_user_id
          LEFT JOIN school_classes sc ON sc.id = sp.class_id
          WHERE l.is_active = true AND sp.school_id = :schoolId
          ORDER BY u.last_name, u.first_name
          """,
      nativeQuery = true)
  List<ChildRow> findActiveChildrenBySchool(@Param("schoolId") UUID schoolId);

  @Query(
      value =
          """
          SELECT l.parent_user_id AS parentUserId, l.student_user_id AS studentUserId,
                 l.relation AS relation, u.first_name AS firstName, u.last_name AS lastName,
                 sc.full_name AS className
          FROM parent_student_links l
          JOIN users u ON u.id = l.student_user_id
          JOIN student_profiles sp ON sp.user_id = l.student_user_id
          LEFT JOIN school_classes sc ON sc.id = sp.class_id
          WHERE l.is_active = true AND l.parent_user_id = :parentUserId
          ORDER BY u.last_name, u.first_name
          """,
      nativeQuery = true)
  List<ChildRow> findActiveChildrenByParent(@Param("parentUserId") UUID parentUserId);
}
