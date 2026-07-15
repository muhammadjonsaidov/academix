package uz.academixai.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;

/**
 * Resolves which school a user belongs to, for embedding in the JWT (the RLS interceptor reads this
 * claim to set {@code app.current_school_id} per request — see CLAUDE.md "Backend architecture").
 *
 * <p>ADMIN via {@code schools.admin_id}, TEACHER via {@code users.school_id} (the Sprint-1
 * deviation column, see V6 migration), STUDENT via {@code student_profiles.school_id}. PARENT (via
 * {@code parent_student_links} -> {@code student_profiles.school_id}) is still unresolved — that
 * table doesn't exist yet and no PARENT-facing endpoint touches an RLS table this sprint.
 * TEACHER/STUDENT resolution was the real prerequisite this comment used to flag ("must be
 * implemented before the Homework epic ships") — this sprint IS that epic.
 */
@Component
public class SchoolContextResolver {

  private final SchoolRepository schoolRepository;
  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;

  public SchoolContextResolver(
      SchoolRepository schoolRepository,
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository) {
    this.schoolRepository = schoolRepository;
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
  }

  public Optional<UUID> resolve(User user) {
    return switch (user.role()) {
      case ADMIN -> schoolRepository.findByAdminId(user.id()).map(school -> school.getId());
      case TEACHER -> userRepository.findById(user.id()).map(entity -> entity.getSchoolId());
      case STUDENT ->
          studentProfileRepository.findByUserId(user.id()).map(profile -> profile.getSchoolId());
      case PARENT, PSYCHOLOGIST -> Optional.empty();
    };
  }
}
