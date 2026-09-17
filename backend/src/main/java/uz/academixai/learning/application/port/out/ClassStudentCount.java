package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for a class' stored student count.
 *
 * <p>Deliberately not {@link ClassSizeQuery}: that one counts students that are active right now,
 * this one reads the denormalised {@code school_classes.student_count} column the teacher analytics
 * submission-rate maths uses. They are different numbers, and swapping one for the other would
 * change the percentages without changing any code that looks wrong.
 */
public interface ClassStudentCount {

  Optional<Integer> of(UUID classId, UUID schoolId);
}
