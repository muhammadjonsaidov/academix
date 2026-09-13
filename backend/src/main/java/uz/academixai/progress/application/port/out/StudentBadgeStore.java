package uz.academixai.progress.application.port.out;

import java.util.UUID;
import uz.academixai.progress.domain.StudentBadge;

/** Student badge ownership boundary. */
public interface StudentBadgeStore {

  boolean existsByStudentIdAndBadgeId(UUID studentId, UUID badgeId);

  StudentBadge save(StudentBadge badge);
}
