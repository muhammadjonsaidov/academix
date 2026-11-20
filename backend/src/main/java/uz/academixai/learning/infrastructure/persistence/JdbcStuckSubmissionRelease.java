package uz.academixai.learning.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;
import uz.academixai.learning.application.port.out.StuckSubmissionRelease;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * One native update over every school's submissions. The sweep is cross-tenant by definition (a
 * scheduled job, no request, no school), so it runs in the system scope.
 */
@Repository
public class JdbcStuckSubmissionRelease implements StuckSubmissionRelease {

  private static final String RELEASE =
      """
      UPDATE homework_submissions SET status = 'AI_SKIPPED'
      WHERE status IN ('SUBMITTED', 'AI_PROCESSING') AND submitted_at < :cutoff
      """;

  private final EntityManager entityManager;
  private final TenantScope tenantScope;

  public JdbcStuckSubmissionRelease(EntityManager entityManager, TenantScope tenantScope) {
    this.entityManager = entityManager;
    this.tenantScope = tenantScope;
  }

  @Override
  public int releaseSubmittedBefore(LocalDateTime cutoff) {
    return tenantScope.callAsSystem(
        () ->
            entityManager
                .createNativeQuery(RELEASE)
                .setParameter("cutoff", Timestamp.valueOf(cutoff))
                .executeUpdate());
  }
}
