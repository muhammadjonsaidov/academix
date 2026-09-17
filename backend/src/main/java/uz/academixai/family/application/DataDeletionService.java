package uz.academixai.family.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.StudentProfile;
import uz.academixai.family.application.port.out.ChildReadModel;
import uz.academixai.family.application.port.out.DeletionRequestStore;
import uz.academixai.family.application.port.out.MinorDataEraser;
import uz.academixai.family.application.port.out.ParentLinkStore;
import uz.academixai.family.domain.DataDeletionRequest;
import uz.academixai.family.domain.DeletionRequestStatus;
import uz.academixai.shared.error.ApiException;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * academix_tz.md §5.6 / §7.6 — retention for minors' biometric/psychological data. Real pseudocode
 * is given verbatim (backend_tdd.md §7.6), followed exactly.
 *
 * <p><b>Sprint 8 update:</b> {@link #requestDeletion} (backing §2.7's {@code POST
 * /parent/children/{studentId}/data-deletion-request}) is now built — Sprint 7 deferred it because
 * it needs {@code parent_student_links} to verify "is this really your child" before accepting a
 * request, and that table didn't exist yet. It does now.
 */
@Service
public class DataDeletionService {

  private static final Logger log = LoggerFactory.getLogger(DataDeletionService.class);

  private final DeletionRequestStore requests;
  private final ParentLinkStore links;
  private final ChildReadModel children;
  private final MinorDataEraser eraser;
  private final TenantScope tenantScope;

  public DataDeletionService(
      DeletionRequestStore requests,
      ParentLinkStore links,
      ChildReadModel children,
      MinorDataEraser eraser,
      TenantScope tenantScope) {
    this.requests = requests;
    this.links = links;
    this.children = children;
    this.eraser = eraser;
    this.tenantScope = tenantScope;
  }

  /**
   * academix_tz.md §2.5 — {@code POST /parent/children/{studentId}/data-deletion-request}, "only if
   * student isActive=false" (read as {@code student_profiles.is_active} — a student who has left
   * the school, e.g. graduated/transferred, not a currently-enrolled one). Unblocks Sprint 7's
   * deferred gap now that {@code parent_student_links} exists to verify "is this really your child"
   * before accepting a request.
   */
  public DataDeletionRequest requestDeletion(UUID parentUserId, UUID studentId) {
    if (!links.existsActive(parentUserId, studentId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu farzandingiz emas.",
          "Faqat o'zingizga bog'langan farzandlar uchun so'rov yuborishingiz mumkin.");
    }
    StudentProfile profile =
        children
            .profile(studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_STUDENT_NOT_FOUND",
                        "O'quvchi topilmadi.",
                        "ID ni tekshiring."));
    if (profile.isActive()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_STUDENT_STILL_ACTIVE",
          "Faol o'quvchi uchun ma'lumotlarni o'chirish so'rovi yuborib bo'lmaydi.",
          "O'quvchi maktabni tark etgandan so'ng qayta urinib ko'ring.");
    }

    return requests.save(
        new DataDeletionRequest(
            UUID.randomUUID(),
            profile.schoolId(),
            studentId,
            parentUserId,
            DeletionRequestStatus.PENDING,
            LocalDateTime.now(),
            null));
  }

  public List<DataDeletionRequest> listPending(UUID schoolId) {
    return requests.findPendingOfSchool(schoolId);
  }

  /**
   * backend_tdd.md §7.6's exact pseudocode: nulls handwriting {@code feature_vector}/{@code
   * is_reliable} and psychological_signals' {@code raw_evidence}/{@code description} for the
   * student — rows survive (audit/aggregation), only the sensitive content disappears.
   */
  // noRollbackFor per the CLAUDE.md rule: this participates in the request's transaction (the
  // RLS filter wraps every request) and throws a business ApiException (404) before any write —
  // without it the RLS transaction goes rollback-only and the request dies with an
  // UnexpectedRollbackException 500 instead of the intended 404 (confirmed by a real test run).
  @Transactional(noRollbackFor = ApiException.class)
  public DataDeletionRequest approveDataDeletion(UUID schoolId, UUID requestId) {
    DataDeletionRequest request =
        requests
            .findByIdInSchool(requestId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "So'rov topilmadi.",
                        "ID ni tekshiring."));

    eraser.eraseHandwritingProfile(request.studentId());
    eraser.erasePsychologicalEvidence(request.studentId());

    return requests.save(
        new DataDeletionRequest(
            request.id(),
            request.schoolId(),
            request.studentId(),
            request.requestedBy(),
            DeletionRequestStatus.APPROVED,
            request.requestedAt(),
            LocalDateTime.now()));
  }

  /**
   * Monthly retention job: a resolved signal older than two years loses its evidence. Cross-tenant
   * by definition — it sweeps every school in one statement — so it runs as the system role rather
   * than pretending to be scoped (and so it keeps working once Wellbeing's signals table is
   * RLS-protected).
   */
  @Scheduled(cron = "0 0 3 1 * *")
  @Transactional
  public void anonymizeOldResolvedSignals() {
    int updated = tenantScope.callAsSystem(eraser::anonymizeResolvedSignalsOlderThanTwoYears);
    log.info("Monthly psychological-signal anonymization: {} rows anonymized", updated);
  }
}
