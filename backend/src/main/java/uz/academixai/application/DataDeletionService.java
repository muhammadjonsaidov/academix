package uz.academixai.application;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.DataDeletionRequest;
import uz.academixai.domain.DeletionRequestStatus;
import uz.academixai.infrastructure.persistence.DataDeletionRequestEntity;
import uz.academixai.infrastructure.persistence.DataDeletionRequestRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §5.6 / §7.6 — retention for minors' biometric/psychological data. Real pseudocode
 * is given verbatim (backend_tdd.md §7.6), followed exactly.
 *
 * <p><b>Real, flagged gap:</b> §2.7's {@code POST
 * /parent/children/{studentId}/data-deletion-request} (the creation endpoint) is NOT built here —
 * it needs {@code parent_student_links} to verify the requesting parent is actually linked to that
 * student, and that table doesn't exist anywhere in this codebase yet (the exact same pre-existing
 * gap {@link SchoolContextResolver}'s Javadoc already documents for PARENT resolution generally).
 * Building a parent-facing creation endpoint with no way to check "is this really your child" would
 * be a real authorization hole, not a shortcut — deferred until that link table exists. This sprint
 * builds the admin-side (list pending, approve) and the actual retention/nulling logic, which don't
 * depend on it — a request row can still be inserted directly (e.g. by a future admin tool) and
 * processed correctly.
 */
@Service
public class DataDeletionService {

  private final DataDeletionRequestRepository requestRepository;
  private final EntityManager entityManager;
  private static final Logger log = LoggerFactory.getLogger(DataDeletionService.class);

  public DataDeletionService(
      DataDeletionRequestRepository requestRepository, EntityManager entityManager) {
    this.requestRepository = requestRepository;
    this.entityManager = entityManager;
  }

  public List<DataDeletionRequest> listPending(UUID schoolId) {
    return requestRepository
        .findBySchoolIdAndStatusOrderByRequestedAtDesc(schoolId, DeletionRequestStatus.PENDING)
        .stream()
        .map(DataDeletionRequestEntity::toDomain)
        .toList();
  }

  /**
   * backend_tdd.md §7.6's exact pseudocode: nulls handwriting {@code feature_vector}/{@code
   * is_reliable} and psychological_signals' {@code raw_evidence}/{@code description} for the
   * student — rows survive (audit/aggregation), only the sensitive content disappears.
   */
  @Transactional
  public DataDeletionRequest approveDataDeletion(UUID schoolId, UUID requestId) {
    DataDeletionRequestEntity entity =
        requestRepository
            .findByIdAndSchoolId(requestId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "So'rov topilmadi.",
                        "ID ni tekshiring."));
    UUID studentId = entity.getStudentId();

    entityManager
        .createNativeQuery(
            "UPDATE handwriting_profiles SET feature_vector = NULL, is_reliable = FALSE WHERE student_id = :studentId")
        .setParameter("studentId", studentId)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "UPDATE psychological_signals SET raw_evidence = NULL, description = NULL WHERE student_id = :studentId")
        .setParameter("studentId", studentId)
        .executeUpdate();

    DataDeletionRequest domain = entity.toDomain();
    DataDeletionRequest approved =
        new DataDeletionRequest(
            domain.id(),
            domain.schoolId(),
            domain.studentId(),
            domain.requestedBy(),
            DeletionRequestStatus.APPROVED,
            domain.requestedAt(),
            LocalDateTime.now());
    return requestRepository.save(DataDeletionRequestEntity.fromDomain(approved)).toDomain();
  }

  // Scheduled, oyiga bir marta — resolved signal 2 yildan eski bo'lsa anonimlashtiriladi.
  @Scheduled(cron = "0 0 3 1 * *")
  @Transactional
  public void anonymizeOldResolvedSignals() {
    int updated =
        entityManager
            .createNativeQuery(
                "UPDATE psychological_signals SET description = NULL, raw_evidence = NULL"
                    + " WHERE resolved = TRUE AND resolved_at < NOW() - INTERVAL '2 years'")
            .executeUpdate();
    log.info("Monthly psychological-signal anonymization: {} rows anonymized", updated);
  }
}
