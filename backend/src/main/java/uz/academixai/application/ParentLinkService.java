package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.ParentRelation;
import uz.academixai.domain.ParentStudentLink;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.ParentStudentLinkEntity;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md — {@code POST /admin/parents/link}. Real gap needing a judgment call: the spec
 * gives the endpoint's request body ({@code parentPhone, studentId, relation}) but no separate
 * parent-invite/self-registration flow exists anywhere — this find-or-creates the parent user by
 * phone in one step. Unlike {@code TeacherManagementService}/{@code
 * PsychologistManagementService}'s invite pattern, a newly-created parent is active immediately (no
 * {@code /activate} step) — the spec gives no equivalent step for parents, and "admin directly
 * links a parent" is the only flow documented at all.
 */
@Service
public class ParentLinkService {

  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final ParentStudentLinkRepository linkRepository;
  private final PasswordEncoder passwordEncoder;

  public ParentLinkService(
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      ParentStudentLinkRepository linkRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.linkRepository = linkRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public ParentStudentLink link(
      UUID schoolId, String parentPhone, UUID studentId, ParentRelation relation) {
    requireStudentInSchool(schoolId, studentId);

    UserEntity parent =
        userRepository.findByPhone(parentPhone).orElseGet(() -> createParent(parentPhone));
    if (parent.getRole() != Role.PARENT) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam boshqa turdagi foydalanuvchiga tegishli.",
          "Boshqa telefon raqam kiriting.");
    }

    ParentStudentLinkEntity existing =
        linkRepository.findByParentUserIdAndStudentUserId(parent.getId(), studentId).orElse(null);
    ParentStudentLink linkDomain =
        new ParentStudentLink(
            existing != null ? existing.getId() : UUID.randomUUID(),
            parent.getId(),
            studentId,
            relation,
            true,
            existing != null && existing.toDomain().biometricConsentGiven(),
            existing != null ? existing.toDomain().consentGivenAt() : null);
    return linkRepository.save(ParentStudentLinkEntity.fromDomain(linkDomain)).toDomain();
  }

  private UserEntity createParent(String phone) {
    UserEntity entity =
        new UserEntity(
            UUID.randomUUID(),
            "Ota-ona",
            "",
            phone,
            null,
            passwordEncoder.encode(TempPasswordGenerator.generate()),
            Role.PARENT,
            true,
            LocalDateTime.now(),
            null,
            null);
    return userRepository.save(entity);
  }

  /**
   * academix_tz.md §5.6 — {@code PUT /parent/children/{studentId}/consent/biometric}. Consent does
   * NOT gate handwriting collection (functional necessity for plagiarism defense per §5.6) — it
   * only gates disclosure to the parent, i.e. this is a transparency flag, not a permission check
   * anywhere in the handwriting pipeline.
   */
  public ParentStudentLink setBiometricConsent(
      UUID parentUserId, UUID studentId, boolean consentGiven) {
    ParentStudentLinkEntity entity =
        linkRepository
            .findByParentUserIdAndStudentUserId(parentUserId, studentId)
            .filter(e -> e.toDomain().isActive())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_STUDENT_NOT_FOUND",
                        "Bu farzandingiz emas.",
                        "Bog'lanish topilmadi."));
    ParentStudentLink domain = entity.toDomain();
    ParentStudentLink updated =
        new ParentStudentLink(
            domain.id(),
            domain.parentUserId(),
            domain.studentUserId(),
            domain.relation(),
            domain.isActive(),
            consentGiven,
            consentGiven ? LocalDateTime.now() : null);
    return linkRepository.save(ParentStudentLinkEntity.fromDomain(updated)).toDomain();
  }

  private void requireStudentInSchool(UUID schoolId, UUID studentId) {
    studentProfileRepository
        .findByUserId(studentId)
        .filter(p -> schoolId.equals(p.getSchoolId()))
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_STUDENT_NOT_FOUND",
                    "O'quvchi topilmadi.",
                    "ID ni tekshiring."));
  }
}
