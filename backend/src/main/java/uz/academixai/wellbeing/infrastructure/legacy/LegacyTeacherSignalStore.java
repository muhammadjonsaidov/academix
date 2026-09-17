package uz.academixai.wellbeing.infrastructure.legacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.wellbeing.application.port.out.TeacherSignalStore;

/** Adapter for {@link TeacherSignalStore} over the legacy signal repository. */
@Component
public class LegacyTeacherSignalStore implements TeacherSignalStore {

  private final PsychologicalSignalRepository signals;

  public LegacyTeacherSignalStore(PsychologicalSignalRepository signals) {
    this.signals = signals;
  }

  @Override
  public List<PsychologicalSignal> signalsOf(List<UUID> studentIds, SignalSeverity severity) {
    var entities =
        severity == null
            ? signals.findByStudentIdInOrderByDetectedAtDesc(studentIds)
            : signals.findByStudentIdInAndSeverityOrderByDetectedAtDesc(studentIds, severity);
    return entities.stream().map(PsychologicalSignalEntity::toDomain).toList();
  }

  @Override
  public Optional<PsychologicalSignal> signalOf(List<UUID> studentIds, UUID signalId) {
    return signals
        .findByIdAndStudentIdIn(signalId, studentIds)
        .map(PsychologicalSignalEntity::toDomain);
  }
}
