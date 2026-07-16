package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.domain.SignalSeverity;

public interface PsychologicalSignalRepository
    extends JpaRepository<PsychologicalSignalEntity, UUID> {

  List<PsychologicalSignalEntity> findByStudentIdInOrderByDetectedAtDesc(List<UUID> studentIds);

  List<PsychologicalSignalEntity> findByStudentIdInAndSeverityOrderByDetectedAtDesc(
      List<UUID> studentIds, SignalSeverity severity);

  List<PsychologicalSignalEntity> findAllByOrderByDetectedAtDesc();

  List<PsychologicalSignalEntity> findBySeverityOrderByDetectedAtDesc(SignalSeverity severity);

  List<PsychologicalSignalEntity> findByResolvedOrderByDetectedAtDesc(boolean resolved);

  List<PsychologicalSignalEntity> findBySeverityAndResolvedOrderByDetectedAtDesc(
      SignalSeverity severity, boolean resolved);

  Optional<PsychologicalSignalEntity> findByIdAndStudentIdIn(UUID id, List<UUID> studentIds);

  int countBySeverityAndResolved(SignalSeverity severity, boolean resolved);

  int countByResolvedAndResolvedAtAfter(boolean resolved, LocalDateTime after);

  List<PsychologicalSignalEntity> findByStudentIdOrderByDetectedAtDesc(UUID studentId);
}
