package uz.academixai.wellbeing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** School-scoped student ownership lookup for behavioral analysis. */
public interface ActiveStudentDirectory {

  record Student(UUID studentId, UUID schoolId) {}

  List<Student> findAllActive();

  Optional<UUID> findSchoolId(UUID studentId);
}
