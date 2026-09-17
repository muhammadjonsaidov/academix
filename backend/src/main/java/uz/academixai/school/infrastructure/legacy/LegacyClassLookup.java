package uz.academixai.school.infrastructure.legacy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.school.application.port.out.ClassLookup;

/**
 * Compatibility adapter over {@code school_classes}: the class administration slice already reads
 * classes through its own port, but this repository stays in the legacy package until that
 * consolidation happens.
 */
@Component
public class LegacyClassLookup implements ClassLookup {

  private final SchoolClassRepository classes;

  public LegacyClassLookup(SchoolClassRepository classes) {
    this.classes = classes;
  }

  @Override
  public boolean existsInSchool(UUID classId, UUID schoolId) {
    return classes.findByIdAndSchoolId(classId, schoolId).isPresent();
  }

  @Override
  public void incrementStudentCount(UUID classId) {
    classes.incrementStudentCount(classId);
  }

  @Override
  public void decrementStudentCount(UUID classId) {
    classes.decrementStudentCount(classId);
  }
}
