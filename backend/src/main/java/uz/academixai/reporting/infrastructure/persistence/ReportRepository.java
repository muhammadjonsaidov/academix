package uz.academixai.reporting.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.reporting.domain.ReportType;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

  List<ReportEntity> findBySchoolIdOrderByGeneratedAtDesc(UUID schoolId);

  Optional<ReportEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  /**
   * {@code GET /parent/children/{id}/quarter-report} — reuse a recently-generated STUDENT report
   * rather than regenerating on every call; {@code generatedAfter} bounds "recent".
   */
  Optional<ReportEntity> findFirstByTypeAndTargetIdAndGeneratedAtAfterOrderByGeneratedAtDesc(
      ReportType type, UUID targetId, LocalDateTime generatedAfter);
}
