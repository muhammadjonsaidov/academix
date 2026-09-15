package uz.academixai.school.application;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.school.application.port.in.SchoolMembership;
import uz.academixai.school.application.port.out.MembershipLookup;

/**
 * Resolves the school a user belongs to, per role — the use case that used to live in the legacy
 * {@code application/SchoolContextResolver}.
 *
 * <p>Judgment call for parents, carried over unchanged: a parent may have children at different
 * schools, but a JWT carries exactly one {@code schoolId} claim, so the first active link (ordered
 * by link id, deterministic) decides. A parent with no linked child resolves to empty.
 */
@Service
public class SchoolMembershipService implements SchoolMembership {

  private final MembershipLookup lookup;
  private final ParentChildAccess parentLinks;

  public SchoolMembershipService(MembershipLookup lookup, ParentChildAccess parentLinks) {
    this.lookup = lookup;
    this.parentLinks = parentLinks;
  }

  @Override
  public Optional<UUID> schoolOf(UUID userId, Role role) {
    return switch (role) {
      case ADMIN -> lookup.schoolOfAdmin(userId);
      case TEACHER, PSYCHOLOGIST -> lookup.schoolOfAccount(userId);
      case STUDENT -> lookup.schoolOfStudent(userId);
      case PARENT -> schoolOfParent(userId);
    };
  }

  private Optional<UUID> schoolOfParent(UUID parentUserId) {
    return parentLinks.childrenOf(parentUserId).stream()
        .sorted(Comparator.comparing(ParentStudentLink::id))
        .findFirst()
        .flatMap(link -> lookup.schoolOfStudent(link.studentUserId()));
  }
}
