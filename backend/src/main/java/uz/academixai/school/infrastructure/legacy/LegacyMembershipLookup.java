package uz.academixai.school.infrastructure.legacy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.school.application.port.out.MembershipLookup;

/**
 * Compatibility adapter over the three membership tables, which still live in the legacy
 * persistence package: {@code schools} (naming its admin), {@code users.school_id} and {@code
 * student_profiles.school_id}. Each read disappears as those tables move to their owning contexts.
 */
@Component
public class LegacyMembershipLookup implements MembershipLookup {

  private final SchoolRepository schools;
  private final UserRepository users;
  private final StudentProfileRepository students;

  public LegacyMembershipLookup(
      SchoolRepository schools, UserRepository users, StudentProfileRepository students) {
    this.schools = schools;
    this.users = users;
    this.students = students;
  }

  @Override
  public Optional<UUID> schoolOfAdmin(UUID adminUserId) {
    return schools.findByAdminId(adminUserId).map(school -> school.getId());
  }

  @Override
  public Optional<UUID> schoolOfAccount(UUID userId) {
    return users.findById(userId).map(entity -> entity.getSchoolId());
  }

  @Override
  public Optional<UUID> schoolOfStudent(UUID studentUserId) {
    return students.findByUserId(studentUserId).map(profile -> profile.getSchoolId());
  }
}
