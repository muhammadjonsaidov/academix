package uz.academixai.wellbeing.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the students a teacher is responsible for as their class teacher.
 *
 * <p>Distinct from {@link ActiveStudentDirectory}, which sweeps every school: this one is narrowed
 * to one teacher, because the teacher-facing signal views are only allowed to show signal data for
 * their own class.
 */
public interface TeacherClassRoster {

  List<UUID> studentIdsOf(UUID schoolId, UUID teacherId);
}
