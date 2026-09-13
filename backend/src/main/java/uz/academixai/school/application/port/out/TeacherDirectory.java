package uz.academixai.school.application.port.out;

import java.util.UUID;

/** Cross-context lookup of an active teacher identity for School administration. */
public interface TeacherDirectory {

  boolean existsTeacher(UUID schoolId, UUID teacherId);
}
