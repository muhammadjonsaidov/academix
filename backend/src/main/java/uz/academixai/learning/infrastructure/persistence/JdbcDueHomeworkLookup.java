package uz.academixai.learning.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.learning.application.port.out.DueHomeworkLookup;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Native query over every school's assignments and submissions. The reminder sweep is cross-tenant
 * by definition (no request, no school), so it runs in the system scope — read-only, one query.
 */
@Repository
public class JdbcDueHomeworkLookup implements DueHomeworkLookup {

  private static final String DUE_UNSUBMITTED =
      """
      SELECT sp.user_id, ha.id, ha.title, ha.deadline_at
      FROM homework_assignments ha
      JOIN student_profiles sp ON sp.class_id = ha.class_id AND sp.school_id = ha.school_id
      JOIN users u ON u.id = sp.user_id AND u.is_active
      WHERE ha.is_active
        AND ha.deadline_at > :from AND ha.deadline_at <= :to
        AND NOT EXISTS (
          SELECT 1 FROM homework_submissions hs
          WHERE hs.assignment_id = ha.id AND hs.student_id = sp.user_id)
      ORDER BY ha.deadline_at
      """;

  private final EntityManager entityManager;
  private final TenantScope tenantScope;

  public JdbcDueHomeworkLookup(EntityManager entityManager, TenantScope tenantScope) {
    this.entityManager = entityManager;
    this.tenantScope = tenantScope;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DueHomework> unsubmittedWithDeadlineBetween(LocalDateTime from, LocalDateTime to) {
    return tenantScope.callAsSystem(
        () ->
            ((List<Object[]>)
                    entityManager
                        .createNativeQuery(DUE_UNSUBMITTED)
                        .setParameter("from", Timestamp.valueOf(from))
                        .setParameter("to", Timestamp.valueOf(to))
                        .getResultList())
                .stream()
                    .map(
                        row ->
                            new DueHomework(
                                (UUID) row[0],
                                (UUID) row[1],
                                (String) row[2],
                                ((Timestamp) row[3]).toLocalDateTime()))
                    .toList());
  }
}
