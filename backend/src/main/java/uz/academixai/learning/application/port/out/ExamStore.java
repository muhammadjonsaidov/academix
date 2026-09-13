package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Exam;

/** Persistence boundary for Learning's exam aggregate. */
public interface ExamStore {

  Exam save(Exam exam);

  List<Exam> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId);

  Optional<Exam> findByIdAndSchoolId(UUID examId, UUID schoolId);
}
