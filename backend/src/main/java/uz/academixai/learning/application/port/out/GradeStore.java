package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Grade;

/** Persistence boundary for the final teacher grade. */
public interface GradeStore {

  Optional<Grade> findBySubmissionId(UUID submissionId);

  Grade save(Grade grade);
}
