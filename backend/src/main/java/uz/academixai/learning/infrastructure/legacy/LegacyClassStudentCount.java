package uz.academixai.learning.infrastructure.legacy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.learning.application.port.out.ClassStudentCount;

/** Adapter for {@link ClassStudentCount} over the legacy class repository. */
@Component
public class LegacyClassStudentCount implements ClassStudentCount {

  private final SchoolClassRepository classes;

  public LegacyClassStudentCount(SchoolClassRepository classes) {
    this.classes = classes;
  }

  @Override
  public Optional<Integer> of(UUID classId, UUID schoolId) {
    return classes.findByIdAndSchoolId(classId, schoolId).map(SchoolClassEntity::getStudentCount);
  }
}
