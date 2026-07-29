package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository.ChildRow;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * Admin-facing parent (ota-ona/vasiy) management — create with an admin-supplied password, list
 * with linked children, and a pre-link phone check.
 *
 * <p>DEVIATION, documented judgment call (same category as ParentLinkService's find-or-create):
 * academix_tz.md §2.2's only parent endpoint is {@code POST /admin/parents/link} — no create, no
 * list, no check are documented anywhere. In practice that made parents invisible: link's
 * find-or-create stored them with a hardcoded name, no email, no school scoping, and no UI could
 * ever list them again. This service adds the missing lifecycle:
 *
 * <ul>
 *   <li>{@code create} requires phone AND email together (email is what password reset uses — a
 *       parent with phone but no email can never recover their account) and takes the initial
 *       password from the admin, who hands it to the parent; the parent may change it later via the
 *       normal {@code PUT /auth/change-password}. This deliberately differs from the
 *       teacher/psychologist invite pattern (server-generated temp password, inactive until
 *       /activate) because no credential-delivery channel exists — the admin IS the delivery
 *       channel here, so the account is active immediately.
 *   <li>Parents get {@code users.school_id} set (the V6 deviation column, same as TEACHER) so a
 *       school-scoped list is possible at all.
 * </ul>
 */
@Service
public class ParentManagementService {

  private final UserRepository userRepository;
  private final ParentStudentLinkRepository linkRepository;
  private final PasswordEncoder passwordEncoder;

  public ParentManagementService(
      UserRepository userRepository,
      ParentStudentLinkRepository linkRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.linkRepository = linkRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User create(
      UUID schoolId,
      String firstName,
      String lastName,
      String phone,
      String email,
      String password) {
    requireNotBlank(firstName, "Ism majburiy.");
    requireNotBlank(phone, "Telefon raqam majburiy.");
    // Email is deliberately REQUIRED for parents (unlike the teacher invite's optional email):
    // password reset is email-based, and a phone-only parent account is unrecoverable.
    requireNotBlank(email, "Email majburiy — parolni tiklash email orqali ishlaydi.");
    requireNotBlank(password, "Boshlang'ich parol majburiy.");
    if (password.length() < 8) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_VALIDATION",
          "Parol kamida 8 belgidan iborat bo'lishi kerak.",
          "Uzunroq parol kiriting.");
    }
    if (userRepository.existsByPhone(phone)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam allaqachon ro'yxatdan o'tgan.",
          "Boshqa telefon raqam kiriting yoki mavjud foydalanuvchini tekshiring.");
    }

    UserEntity entity =
        new UserEntity(
            UUID.randomUUID(),
            firstName,
            lastName == null ? "" : lastName,
            phone,
            email,
            passwordEncoder.encode(password),
            Role.PARENT,
            true,
            LocalDateTime.now(),
            null,
            schoolId);
    return userRepository.save(entity).toDomain();
  }

  /** Parents of this school with their actively-linked children (may be empty). */
  public List<ParentWithChildren> list(UUID schoolId) {
    Map<UUID, List<ChildRow>> childrenByParent =
        linkRepository.findActiveChildrenBySchool(schoolId).stream()
            .collect(Collectors.groupingBy(ChildRow::getParentUserId));
    return userRepository
        .findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role.PARENT, schoolId)
        .stream()
        .map(
            e ->
                new ParentWithChildren(
                    e.toDomain(), childrenByParent.getOrDefault(e.getId(), List.of())))
        .toList();
  }

  /**
   * Pre-link lookup so the admin can SEE the state of a phone number before linking: does a user
   * exist, is it already a parent, and which children are already linked. Solves the reported
   * "linked the parent, then it vanished — was it even created?" opacity.
   */
  public ParentCheck check(String phone) {
    UserEntity user = userRepository.findByPhone(phone).orElse(null);
    if (user == null) {
      return new ParentCheck(false, null, null, List.of());
    }
    List<ChildRow> children =
        user.getRole() == Role.PARENT
            ? linkRepository.findActiveChildrenByParent(user.getId())
            : List.of();
    return new ParentCheck(true, user.getRole().name(), user.toDomain(), children);
  }

  private void requireNotBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "ERR_VALIDATION", message, "Barcha maydonlarni to'ldiring.");
    }
  }

  public record ParentWithChildren(User parent, List<ChildRow> children) {}

  public record ParentCheck(boolean exists, String role, User user, List<ChildRow> children) {}
}
