package uz.academixai.learning.infrastructure.legacy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.learning.application.port.out.ClassGradeLookup;

/** Adapter for {@link ClassGradeLookup} over the legacy class repository. */
@Component
public class LegacyClassGradeLookup implements ClassGradeLookup {

  private final SchoolClassRepository classes;

  public LegacyClassGradeLookup(SchoolClassRepository classes) {
    this.classes = classes;
  }

  @Override
  public Optional<Integer> gradeOf(UUID classId) {
    return classes.findById(classId).map(SchoolClassEntity::getGrade);
  }
}
