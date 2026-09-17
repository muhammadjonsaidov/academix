package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Read-side School data needed for a pre-flight exam AI-cost estimate. */
public interface ClassSizeQuery {

  int activeStudentCount(UUID schoolId, UUID classId);
}
