package uz.academixai.wellbeing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.domain.WatchlistEntry;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;
import uz.academixai.wellbeing.application.port.out.PsychologistWorkspaceStore;
import uz.academixai.wellbeing.application.port.out.StudentPresentationLookup;

class PsychologistWorkspaceServiceTest {

  @Test
  void resolveKeepsSignalFactsAndRecordsPsychologistOutcome() {
    UUID schoolId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    PsychologicalSignal original = signal(studentId, false);
    Store store = new Store(schoolId, studentId, original);

    PsychologicalSignal resolved =
        service(store).resolve(schoolId, original.id(), "suhbat", "kuzatish");

    assertThat(resolved.resolved()).isTrue();
    assertThat(resolved.resolutionNotes()).isEqualTo("suhbat");
    assertThat(resolved.actionTaken()).isEqualTo("kuzatish");
    assertThat(resolved.type()).isEqualTo(original.type());
    assertThat(resolved.rawEvidence()).isEqualTo(original.rawEvidence());
  }

  @Test
  void watchlistCannotBeMutatedForStudentOutsidePsychologistsSchool() {
    Store store = new Store(UUID.randomUUID(), UUID.randomUUID(), null);
    PsychologistWorkspace workspace = service(store);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                workspace.addToWatchlist(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "reason"))
        .hasMessageContaining("O'quvchi topilmadi");
    assertThat(store.watchlist).isEmpty();
  }

  @Test
  void dashboardCountsOnlyUnresolvedSignalsAtEachSeverity() {
    UUID schoolId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Store store = new Store(schoolId, studentId, signal(studentId, false));
    store.signals.add(
        new PsychologicalSignal(
            UUID.randomUUID(),
            studentId,
            SignalType.LATE_NIGHT_ACTIVITY,
            SignalSeverity.CRITICAL,
            "x",
            "{}",
            false,
            false,
            false,
            false,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null));

    PsychologistWorkspace.Dashboard dashboard = service(store).dashboard(schoolId);

    assertThat(dashboard.highSignals()).isEqualTo(1);
    assertThat(dashboard.criticalSignals()).isZero();
  }

  private static PsychologistWorkspace service(Store store) {
    StudentPresentationLookup presentations =
        studentId ->
            Optional.of(new StudentPresentationLookup.StudentPresentation("Ali Valiyev", "9-A"));
    BehaviorActivityLookup activity =
        (schoolId, studentId, since, maxMessages) ->
            new BehaviorActivityLookup.Activity(
                List.of(LocalDateTime.now().minusHours(1)),
                5,
                List.of(),
                LocalDate.now(),
                List.of());
    return new PsychologistWorkspaceService(store, presentations, activity);
  }

  private static PsychologicalSignal signal(UUID studentId, boolean resolved) {
    return new PsychologicalSignal(
        UUID.randomUUID(),
        studentId,
        SignalType.NEGATIVE_LANGUAGE,
        SignalSeverity.HIGH,
        "description",
        "{\"evidence\":\"x\"}",
        false,
        true,
        false,
        true,
        resolved,
        LocalDateTime.now(),
        resolved ? LocalDateTime.now() : null,
        null,
        null);
  }

  private static final class Store implements PsychologistWorkspaceStore {

    private final UUID schoolId;
    private final UUID studentId;
    private final List<PsychologicalSignal> signals = new ArrayList<>();
    private final List<WatchlistEntry> watchlist = new ArrayList<>();

    private Store(UUID schoolId, UUID studentId, PsychologicalSignal signal) {
      this.schoolId = schoolId;
      this.studentId = studentId;
      if (signal != null) {
        signals.add(signal);
      }
    }

    @Override
    public List<PsychologicalSignal> listSignals(UUID schoolId, SignalSeverity severity) {
      if (!this.schoolId.equals(schoolId)) {
        return List.of();
      }
      return signals.stream()
          .filter(signal -> severity == null || signal.severity() == severity)
          .toList();
    }

    @Override
    public Optional<PsychologicalSignal> findSignal(UUID schoolId, UUID signalId) {
      return listSignals(schoolId, null).stream()
          .filter(signal -> signal.id().equals(signalId))
          .findFirst();
    }

    @Override
    public PsychologicalSignal saveSignal(PsychologicalSignal signal) {
      signals.removeIf(existing -> existing.id().equals(signal.id()));
      signals.add(signal);
      return signal;
    }

    @Override
    public boolean studentExists(UUID schoolId, UUID studentId) {
      return this.schoolId.equals(schoolId) && this.studentId.equals(studentId);
    }

    @Override
    public List<WatchlistEntry> listWatchlist(UUID schoolId) {
      return this.schoolId.equals(schoolId) ? List.copyOf(watchlist) : List.of();
    }

    @Override
    public Optional<WatchlistEntry> findWatchlistEntry(UUID studentId) {
      return watchlist.stream().filter(entry -> entry.studentId().equals(studentId)).findFirst();
    }

    @Override
    public WatchlistEntry saveWatchlistEntry(WatchlistEntry entry) {
      watchlist.removeIf(existing -> existing.id().equals(entry.id()));
      watchlist.add(entry);
      return entry;
    }

    @Override
    public void deleteWatchlistEntry(UUID studentId) {
      watchlist.removeIf(entry -> entry.studentId().equals(studentId));
    }
  }
}
