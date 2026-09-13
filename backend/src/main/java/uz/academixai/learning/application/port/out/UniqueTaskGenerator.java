package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Starts asynchronous per-student task generation for a unique homework assignment. */
public interface UniqueTaskGenerator {

  void generate(UUID schoolId, UUID teacherId, UUID assignmentId);
}
