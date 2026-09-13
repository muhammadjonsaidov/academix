package uz.academixai.school.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.SchoolClass;

/** Persistence boundary for School's class-administration operations. */
public interface ClassAdministrationRepository {

  List<SchoolClass> findBySchoolId(UUID schoolId);

  boolean existsBySchoolIdAndGradeAndLetter(UUID schoolId, int grade, String letter);

  Optional<SchoolClass> findByIdAndSchoolId(UUID classId, UUID schoolId);

  SchoolClass save(SchoolClass schoolClass);

  void deleteById(UUID classId);
}
