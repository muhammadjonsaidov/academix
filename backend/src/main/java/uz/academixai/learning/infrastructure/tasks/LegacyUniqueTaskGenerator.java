package uz.academixai.learning.infrastructure.tasks;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.UniqueTaskGenerationService;
import uz.academixai.learning.application.port.out.UniqueTaskGenerator;

/** Explicit compatibility adapter until unique-task generation moves into Learning. */
@Component
public class LegacyUniqueTaskGenerator implements UniqueTaskGenerator {

  private final UniqueTaskGenerationService legacyService;

  public LegacyUniqueTaskGenerator(UniqueTaskGenerationService legacyService) {
    this.legacyService = legacyService;
  }

  @Override
  public void generate(UUID schoolId, UUID teacherId, UUID assignmentId) {
    legacyService.generateUniqueTasks(schoolId, teacherId, assignmentId);
  }
}
