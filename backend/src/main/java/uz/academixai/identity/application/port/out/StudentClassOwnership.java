package uz.academixai.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for the ownership question behind class-teacher-assisted student resets.
 *
 * <p>Two fact-lookups rather than one {@code boolean isClassTeacherOf(...)}: the use case answers
 * differently for "no such student here", "no such class" (both 404) and "not your class" (403),
 * and a boolean would collapse three distinct responses into two. The facts come back, the decision
 * stays in the use case.
 */
public interface StudentClassOwnership {

  /** The class this student belongs to in this school; empty when they have no profile here. */
  Optional<UUID> classIdOf(UUID schoolId, UUID studentId);

  /** The class teacher of a class; empty when the class does not exist. */
  Optional<UUID> classTeacherOf(UUID classId);
}
