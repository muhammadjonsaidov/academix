package uz.academixai.school.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Subject;

/** Persistence boundary for a school's subject catalog. */
public interface SubjectCatalogRepository {

  List<Subject> findBySchoolId(UUID schoolId);

  Optional<Subject> findByIdAndSchoolId(UUID subjectId, UUID schoolId);

  boolean existsBySchoolIdAndNameIgnoringCase(UUID schoolId, String name);

  boolean isReferenced(UUID subjectId);

  Subject save(Subject subject);

  void deleteById(UUID subjectId);
}
