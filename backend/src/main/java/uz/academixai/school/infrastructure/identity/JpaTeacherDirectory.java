package uz.academixai.school.infrastructure.identity;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.school.application.port.out.TeacherDirectory;

/** Transitional Identity adapter over the existing users table. */
@Repository
public class JpaTeacherDirectory implements TeacherDirectory {

  private final UserRepository users;

  public JpaTeacherDirectory(UserRepository users) {
    this.users = users;
  }

  @Override
  public boolean existsTeacher(UUID schoolId, UUID teacherId) {
    return users.findByIdAndRoleAndSchoolId(teacherId, Role.TEACHER, schoolId).isPresent();
  }
}
