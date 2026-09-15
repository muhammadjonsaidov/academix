package uz.academixai.family.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.application.port.out.ChildReadModel;
import uz.academixai.family.application.port.out.ParentAccountStore;
import uz.academixai.family.application.port.out.ParentAccountStore.ParentAccount;
import uz.academixai.family.application.port.out.ParentLinkStore;
import uz.academixai.family.domain.ParentRelation;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md — {@code POST /admin/parents/link}. Real gap needing a judgment call: the spec
 * gives the endpoint's request body ({@code parentPhone, studentId, relation}) but no separate
 * parent-invite/self-registration flow exists anywhere — this find-or-creates the parent user by
 * phone in one step. Unlike {@code TeacherManagementService}/{@code
 * PsychologistManagementService}'s invite pattern, a newly-created parent is active immediately (no
 * {@code /activate} step) — the spec gives no equivalent step for parents, and "admin directly
 * links a parent" is the only flow documented at all.
 *
 * <p>Also the Family context's published authorization rule ({@link ParentChildAccess}): "is this
 * really that parent's child" — the question Progress and School ask before exposing a child's
 * data.
 */
@Service
public class ParentLinkService implements ParentChildAccess {

  private final ParentAccountStore accounts;
  private final ParentLinkStore links;
  private final ChildReadModel children;

  public ParentLinkService(
      ParentAccountStore accounts, ParentLinkStore links, ChildReadModel children) {
    this.accounts = accounts;
    this.links = links;
    this.children = children;
  }

  public ParentStudentLink link(
      UUID schoolId, String parentPhone, UUID studentId, ParentRelation relation) {
    requireStudentInSchool(schoolId, studentId);

    ParentAccount parent =
        accounts
            .findByPhone(parentPhone)
            .map(account -> withSchool(account, schoolId))
            .orElseGet(() -> createParent(parentPhone, schoolId));
    if (parent.role() != Role.PARENT) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam boshqa turdagi foydalanuvchiga tegishli.",
          "Boshqa telefon raqam kiriting.");
    }

    ParentStudentLink existing = links.find(parent.id(), studentId).orElse(null);
    ParentStudentLink link =
        new ParentStudentLink(
            existing != null ? existing.id() : UUID.randomUUID(),
            parent.id(),
            studentId,
            relation,
            true,
            existing != null && existing.biometricConsentGiven(),
            existing != null ? existing.consentGivenAt() : null);
    return links.save(link);
  }

  /**
   * Backfills {@code users.school_id} for a parent created before it was set — they were invisible
   * to the school-scoped {@code GET /admin/parents} list, the reported "parent disappears after
   * linking" bug. A no-op when the column already matches.
   */
  private ParentAccount withSchool(ParentAccount account, UUID schoolId) {
    if (schoolId.equals(account.schoolId())) {
      return account;
    }
    return accounts.save(
        new ParentAccount(
            account.id(),
            account.firstName(),
            account.lastName(),
            account.phone(),
            account.email(),
            account.passwordHash(),
            account.role(),
            account.isActive(),
            account.createdAt(),
            account.lastLoginAt(),
            schoolId));
  }

  // Legacy find-or-create path (spec's POST /admin/parents/link with an unknown phone). Prefer
  // creating parents explicitly via POST /admin/parents (ParentManagementService) — that flow has
  // a real name/email/password. Kept for spec conformance; now at least school-scoped so the
  // created parent shows up in GET /admin/parents instead of vanishing.
  private ParentAccount createParent(String phone, UUID schoolId) {
    return accounts.save(
        new ParentAccount(
            UUID.randomUUID(),
            "Ota-ona",
            "",
            phone,
            null,
            accounts.encodePassword(accounts.generateTemporaryPassword()),
            Role.PARENT,
            true,
            LocalDateTime.now(),
            null,
            schoolId));
  }

  /**
   * academix_tz.md §5.6 — {@code PUT /parent/children/{studentId}/consent/biometric}. Consent does
   * NOT gate handwriting collection (functional necessity for plagiarism defense per §5.6) — it
   * only gates disclosure to the parent, i.e. this is a transparency flag, not a permission check
   * anywhere in the handwriting pipeline.
   */
  public ParentStudentLink setBiometricConsent(
      UUID parentUserId, UUID studentId, boolean consentGiven) {
    ParentStudentLink link =
        links
            .find(parentUserId, studentId)
            .filter(ParentStudentLink::isActive)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_STUDENT_NOT_FOUND",
                        "Bu farzandingiz emas.",
                        "Bog'lanish topilmadi."));
    return links.save(
        new ParentStudentLink(
            link.id(),
            link.parentUserId(),
            link.studentUserId(),
            link.relation(),
            link.isActive(),
            consentGiven,
            consentGiven ? LocalDateTime.now() : null));
  }

  /** Shared authorization check other contexts call — "is this really your child". */
  @Override
  public void requireLinkedChild(UUID parentUserId, UUID studentId) {
    if (!links.existsActive(parentUserId, studentId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu farzandingiz emas.",
          "Faqat o'zingizga bog'langan farzandlar ma'lumotlarini ko'rishingiz mumkin.");
    }
  }

  @Override
  public List<ParentStudentLink> childrenOf(UUID parentUserId) {
    return links.findActiveByParent(parentUserId);
  }

  @Override
  public List<ParentStudentLink> parentsOf(UUID studentId) {
    return links.findActiveByStudent(studentId);
  }

  /** Convenience alias kept for the parent-facing use cases in this context. */
  public List<ParentStudentLink> myChildren(UUID parentUserId) {
    return childrenOf(parentUserId);
  }

  private void requireStudentInSchool(UUID schoolId, UUID studentId) {
    children
        .profile(studentId)
        .filter(profile -> schoolId.equals(profile.schoolId()))
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_STUDENT_NOT_FOUND",
                    "O'quvchi topilmadi.",
                    "ID ni tekshiring."));
  }
}
