package uz.academixai.wellbeing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.WatchlistEntry;

/** School-scoped persistence boundary for the psychologist workspace. */
public interface PsychologistWorkspaceStore {

  List<PsychologicalSignal> listSignals(UUID schoolId, SignalSeverity severity);

  Optional<PsychologicalSignal> findSignal(UUID schoolId, UUID signalId);

  PsychologicalSignal saveSignal(PsychologicalSignal signal);

  boolean studentExists(UUID schoolId, UUID studentId);

  List<WatchlistEntry> listWatchlist(UUID schoolId);

  Optional<WatchlistEntry> findWatchlistEntry(UUID studentId);

  WatchlistEntry saveWatchlistEntry(WatchlistEntry entry);

  void deleteWatchlistEntry(UUID studentId);
}
