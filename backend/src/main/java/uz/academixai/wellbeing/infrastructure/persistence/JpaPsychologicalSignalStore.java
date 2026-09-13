package uz.academixai.wellbeing.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.wellbeing.application.port.out.PsychologicalSignalStore;

/** JPA adapter for persisting Wellbeing signals without leaking entities into the use case. */
@Repository
public class JpaPsychologicalSignalStore implements PsychologicalSignalStore {

  private final PsychologicalSignalRepository signals;

  public JpaPsychologicalSignalStore(PsychologicalSignalRepository signals) {
    this.signals = signals;
  }

  @Override
  public PsychologicalSignal save(PsychologicalSignal signal) {
    return signals.save(PsychologicalSignalEntity.fromDomain(signal)).toDomain();
  }
}
