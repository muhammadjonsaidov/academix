package uz.academixai.wellbeing.application.port.out;

import uz.academixai.domain.PsychologicalSignal;

/** Persistence boundary for Wellbeing-owned signals. */
public interface PsychologicalSignalStore {

  PsychologicalSignal save(PsychologicalSignal signal);
}
