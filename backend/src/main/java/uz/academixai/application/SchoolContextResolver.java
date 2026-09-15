package uz.academixai.application;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.User;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;

/**
 * Resolves which school a user belongs to, for embedding in the JWT (the RLS interceptor reads this
 * claim to set {@code app.current_school_id} per request — see CLAUDE.md "Backend architecture").
 *
 * <p>ADMIN via {@code schools.admin_id}, TEACHER/PSYCHOLOGIST via {@code users.school_id} (the
 * Sprint-1 deviation column, see V6 migration), STUDENT via {@code student_profiles.school_id}.
 *
 * <p>PARENT resolves via {@code parent_student_links} -> the linked student's {@code
 * student_profiles.school_id} (Sprint 8). Judgment call: a parent could in principle have children
 * at different schools, but the JWT carries exactly one {@code schoolId} claim — this takes the
 * first active link found (arbitrary but deterministic per-query-order), same shape as every other
 * "collapse a list to one value" judgment call already made in this codebase. A parent with zero
 * linked children (not yet linked by any admin) still resolves to empty, same as before.
 */
@Component
public class SchoolContextResolver {

  private final SchoolRepository schoolRepository;
  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final ParentChildAccess parentLinks;

  public SchoolContextResolver(
      SchoolRepository schoolRepository,
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      ParentChildAccess parentLinks) {
    this.schoolRepository = schoolRepository;
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.parentLinks = parentLinks;
  }

  public Optional<UUID> resolve(User user) {
    return switch (user.role()) {
      case ADMIN -> schoolRepository.findByAdminId(user.id()).map(school -> school.getId());
      case TEACHER, PSYCHOLOGIST ->
          userRepository.findById(user.id()).map(entity -> entity.getSchoolId());
      case STUDENT ->
          studentProfileRepository.findByUserId(user.id()).map(profile -> profile.getSchoolId());
      case PARENT -> resolveParent(user.id());
    };
  }

  private Optional<UUID> resolveParent(UUID parentUserId) {
    return parentLinks.childrenOf(parentUserId).stream()
        .sorted(Comparator.comparing(ParentStudentLink::id))
        .findFirst()
        .flatMap(link -> studentProfileRepository.findByUserId(link.studentUserId()))
        .map(profile -> profile.getSchoolId());
  }
}
