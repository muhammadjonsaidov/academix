package uz.academixai.progress.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.StudentProfile;

/** Student progress snapshot persistence boundary. */
public interface StudentProgressProfileStore {

  Optional<StudentProfile> findByStudentId(UUID studentId);

  StudentProfile save(StudentProfile profile);
}
