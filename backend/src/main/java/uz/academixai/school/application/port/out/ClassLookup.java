package uz.academixai.school.application.port.out;

import java.util.UUID;

/**
 * The class facts student management needs: does this class belong to this school, and the roster
 * counters that must stay in step with enrollment.
 */
public interface ClassLookup {

  boolean existsInSchool(UUID classId, UUID schoolId);

  /** {@code school_classes.student_count} is a denormalised counter, not a derived count. */
  void incrementStudentCount(UUID classId);

  void decrementStudentCount(UUID classId);
}
