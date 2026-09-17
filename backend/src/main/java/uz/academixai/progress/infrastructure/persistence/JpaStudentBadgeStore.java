package uz.academixai.progress.infrastructure.persistence;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.progress.application.port.out.StudentBadgeStore;
import uz.academixai.progress.domain.StudentBadge;

/** JPA adapter for student badge ownership. */
@Repository
public class JpaStudentBadgeStore implements StudentBadgeStore {

  private final StudentBadgeRepository badges;

  public JpaStudentBadgeStore(StudentBadgeRepository badges) {
    this.badges = badges;
  }

  @Override
  public boolean existsByStudentIdAndBadgeId(UUID studentId, UUID badgeId) {
    return badges.existsByStudentIdAndBadgeId(studentId, badgeId);
  }

  @Override
  public StudentBadge save(StudentBadge badge) {
    return badges.save(StudentBadgeEntity.fromDomain(badge)).toDomain();
  }
}
