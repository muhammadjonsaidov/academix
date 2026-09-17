package uz.academixai.school.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.StudentProfile;

/** Persistence port for {@code student_profiles} — School owns the student roster. */
public interface StudentStore {

  List<StudentRow> search(UUID schoolId, UUID classId, String search);

  Optional<StudentProfile> findInSchool(UUID studentUserId, UUID schoolId);

  StudentProfile save(StudentProfile profile);

  /**
   * A roster row as the admin list renders it: profile fields joined with the student's account
   * name and phone. A port-local record instead of the JPA projection, so the shape is School's
   * contract rather than a repository detail.
   */
  record StudentRow(
      UUID userId,
      String firstName,
      String lastName,
      String phone,
      UUID classId,
      String studentNumber,
      LocalDate birthDate,
      boolean isActive) {}
}
