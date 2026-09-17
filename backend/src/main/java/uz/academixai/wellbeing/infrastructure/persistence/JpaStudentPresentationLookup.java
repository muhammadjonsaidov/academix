package uz.academixai.wellbeing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.wellbeing.application.port.out.StudentPresentationLookup;

/** JPA adapter for psychologist-facing student labels. */
@Repository
public class JpaStudentPresentationLookup implements StudentPresentationLookup {

  private final UserRepository users;
  private final StudentProfileRepository students;
  private final SchoolClassRepository classes;

  public JpaStudentPresentationLookup(
      UserRepository users, StudentProfileRepository students, SchoolClassRepository classes) {
    this.users = users;
    this.students = students;
    this.classes = classes;
  }

  @Override
  public Optional<StudentPresentation> find(UUID studentId) {
    Optional<UserEntity> user = users.findById(studentId);
    if (user.isEmpty()) {
      return Optional.empty();
    }
    String className =
        students
            .findByUserId(studentId)
            .map(StudentProfileEntity::getClassId)
            .flatMap(classes::findById)
            .map(SchoolClassEntity::getFullName)
            .orElse("");
    return Optional.of(
        new StudentPresentation(
            user.get().getFirstName() + " " + user.get().getLastName(), className));
  }
}
