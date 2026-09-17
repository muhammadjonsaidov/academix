package uz.academixai.school.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.School;

/** Persistence boundary for School's administrative aggregate operations. */
public interface SchoolAdministrationRepository {

  Optional<School> findById(UUID schoolId);

  long countActiveClasses(UUID schoolId);

  School save(School school);
}
