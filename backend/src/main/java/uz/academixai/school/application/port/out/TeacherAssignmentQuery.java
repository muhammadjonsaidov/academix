package uz.academixai.school.application.port.out;

import java.util.Set;
import java.util.UUID;

/** Reads teacher-to-class-to-subject membership owned by the School context. */
public interface TeacherAssignmentQuery {

  Set<UUID> classIds(UUID schoolId, UUID teacherId);

  Set<UUID> subjectIds(UUID schoolId, UUID teacherId);

  boolean isAssignedToClass(UUID schoolId, UUID teacherId, UUID classId);

  boolean isAssignedToClassAndSubject(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);
}
