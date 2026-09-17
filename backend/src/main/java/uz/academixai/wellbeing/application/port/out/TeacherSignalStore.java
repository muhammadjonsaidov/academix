package uz.academixai.wellbeing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;

/**
 * Outbound port for the signal reads a class teacher is allowed to make.
 *
 * <p>Every method takes the roster rather than a school id: the teacher view is scoped to their own
 * class, and pushing that restriction into the query means a caller cannot widen it by accident.
 * The psychologist workspace has its own school-scoped port for the wider view.
 */
public interface TeacherSignalStore {

  List<PsychologicalSignal> signalsOf(List<UUID> studentIds, SignalSeverity severity);

  Optional<PsychologicalSignal> signalOf(List<UUID> studentIds, UUID signalId);
}
