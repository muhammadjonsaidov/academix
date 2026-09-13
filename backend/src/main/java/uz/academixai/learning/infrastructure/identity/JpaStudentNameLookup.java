package uz.academixai.learning.infrastructure.identity;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.learning.application.port.out.StudentNameLookup;

/** Transitional Identity adapter for the review screen's display-only student name. */
@Repository
public class JpaStudentNameLookup implements StudentNameLookup {

  private final UserRepository users;

  public JpaStudentNameLookup(UserRepository users) {
    this.users = users;
  }

  @Override
  public String fullName(UUID studentId) {
    return users
        .findById(studentId)
        .map(user -> user.getFirstName() + " " + user.getLastName())
        .orElse("Noma'lum");
  }
}
