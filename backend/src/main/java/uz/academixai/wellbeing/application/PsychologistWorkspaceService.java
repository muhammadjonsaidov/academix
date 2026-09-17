package uz.academixai.wellbeing.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.WatchlistEntry;
import uz.academixai.shared.error.ApiException;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;
import uz.academixai.wellbeing.application.port.out.PsychologistWorkspaceStore;
import uz.academixai.wellbeing.application.port.out.StudentPresentationLookup;

/** Wellbeing application service for all psychologist dashboard and intervention workflows. */
@Service
public class PsychologistWorkspaceService implements PsychologistWorkspace {

  private static final int TREND_DAYS = 14;

  private final PsychologistWorkspaceStore workspace;
  private final StudentPresentationLookup students;
  private final BehaviorActivityLookup activity;

  public PsychologistWorkspaceService(
      PsychologistWorkspaceStore workspace,
      StudentPresentationLookup students,
      BehaviorActivityLookup activity) {
    this.workspace = workspace;
    this.students = students;
    this.activity = activity;
  }

  @Override
  public Dashboard dashboard(UUID schoolId) {
    List<PsychologicalSignal> signals = workspace.listSignals(schoolId, null);
    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    return new Dashboard(
        unresolvedCount(signals, SignalSeverity.CRITICAL),
        unresolvedCount(signals, SignalSeverity.HIGH),
        unresolvedCount(signals, SignalSeverity.MEDIUM),
        signals.stream()
            .filter(PsychologicalSignal::resolved)
            .filter(signal -> signal.resolvedAt() != null && signal.resolvedAt().isAfter(weekAgo))
            .count(),
        watchlist(schoolId));
  }

  @Override
  public List<SignalWithStudent> listSignals(
      UUID schoolId, SignalSeverity severity, Boolean resolved) {
    return workspace.listSignals(schoolId, severity).stream()
        .filter(signal -> resolved == null || signal.resolved() == resolved)
        .map(this::withStudent)
        .toList();
  }

  @Override
  public SignalDetail getSignalDetail(UUID schoolId, UUID signalId) {
    PsychologicalSignal signal = requireSignal(schoolId, signalId);
    return new SignalDetail(withStudent(signal), behaviorProfile(schoolId, signal.studentId()));
  }

  @Override
  public PsychologicalSignal resolve(
      UUID schoolId, UUID signalId, String notes, String actionTaken) {
    PsychologicalSignal signal = requireSignal(schoolId, signalId);
    return workspace.saveSignal(
        new PsychologicalSignal(
            signal.id(),
            signal.studentId(),
            signal.type(),
            signal.severity(),
            signal.description(),
            signal.rawEvidence(),
            signal.isManipulation(),
            signal.notifiedClassTeacher(),
            signal.notifiedParent(),
            signal.notifiedPsychologist(),
            true,
            signal.detectedAt(),
            LocalDateTime.now(),
            notes,
            actionTaken));
  }

  @Override
  public PsychologicalSignal markManipulation(UUID schoolId, UUID signalId) {
    PsychologicalSignal signal = requireSignal(schoolId, signalId);
    return workspace.saveSignal(
        new PsychologicalSignal(
            signal.id(),
            signal.studentId(),
            signal.type(),
            signal.severity(),
            signal.description(),
            signal.rawEvidence(),
            true,
            signal.notifiedClassTeacher(),
            signal.notifiedParent(),
            signal.notifiedPsychologist(),
            signal.resolved(),
            signal.detectedAt(),
            signal.resolvedAt(),
            signal.resolutionNotes(),
            signal.actionTaken()));
  }

  @Override
  public List<WatchlistEntryWithStudent> watchlist(UUID schoolId) {
    return workspace.listWatchlist(schoolId).stream()
        .map(entry -> new WatchlistEntryWithStudent(entry, studentName(entry.studentId())))
        .toList();
  }

  @Override
  public WatchlistEntry addToWatchlist(
      UUID schoolId, UUID psychologistId, UUID studentId, String reason) {
    requireStudent(schoolId, studentId);
    WatchlistEntry entry =
        workspace
            .findWatchlistEntry(studentId)
            .map(
                existing ->
                    new WatchlistEntry(
                        existing.id(), studentId, psychologistId, reason, existing.addedAt()))
            .orElse(
                new WatchlistEntry(
                    UUID.randomUUID(), studentId, psychologistId, reason, LocalDateTime.now()));
    return workspace.saveWatchlistEntry(entry);
  }

  @Override
  public void removeFromWatchlist(UUID schoolId, UUID studentId) {
    requireStudent(schoolId, studentId);
    workspace.deleteWatchlistEntry(studentId);
  }

  @Override
  public MonthlyReport monthlyReport(UUID schoolId) {
    LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
    List<PsychologicalSignal> signals =
        workspace.listSignals(schoolId, null).stream()
            .filter(signal -> signal.detectedAt().isAfter(monthAgo))
            .toList();
    Map<String, Long> bySeverity = new LinkedHashMap<>();
    Map<String, Long> byType = new LinkedHashMap<>();
    for (PsychologicalSignal signal : signals) {
      bySeverity.merge(signal.severity().name(), 1L, Long::sum);
      byType.merge(signal.type().name(), 1L, Long::sum);
    }
    return new MonthlyReport(
        signals.size(),
        bySeverity,
        byType,
        signals.stream().filter(PsychologicalSignal::resolved).count(),
        signals.stream().filter(PsychologicalSignal::isManipulation).count());
  }

  private BehaviorProfile behaviorProfile(UUID schoolId, UUID studentId) {
    BehaviorActivityLookup.Activity snapshot =
        activity.find(schoolId, studentId, LocalDateTime.now().minusDays(TREND_DAYS), 1);
    Map<String, Integer> activeHours = new LinkedHashMap<>();
    for (int hour = 0; hour < 24; hour += 2) {
      activeHours.put(hour + "-" + (hour + 2), 0);
    }
    Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
    snapshot
        .submissionTimes()
        .forEach(
            time -> {
              int bucketStart = (time.getHour() / 2) * 2;
              activeHours.merge(bucketStart + "-" + (bucketStart + 2), 1, Integer::sum);
              byDay.merge(time.toLocalDate(), 1, Integer::sum);
            });
    return new BehaviorProfile(
        activeHours,
        snapshot.xpHistory(),
        byDay.entrySet().stream()
            .map(entry -> new SubmissionDay(entry.getKey(), entry.getValue()))
            .toList(),
        List.of());
  }

  private SignalWithStudent withStudent(PsychologicalSignal signal) {
    StudentPresentationLookup.StudentPresentation student =
        students
            .find(signal.studentId())
            .orElse(new StudentPresentationLookup.StudentPresentation("", ""));
    return new SignalWithStudent(signal, student.fullName(), student.className());
  }

  private String studentName(UUID studentId) {
    return students
        .find(studentId)
        .map(StudentPresentationLookup.StudentPresentation::fullName)
        .orElse("");
  }

  private PsychologicalSignal requireSignal(UUID schoolId, UUID signalId) {
    return workspace
        .findSignal(schoolId, signalId)
        .orElseThrow(PsychologistWorkspaceService::signalNotFound);
  }

  private void requireStudent(UUID schoolId, UUID studentId) {
    if (!workspace.studentExists(schoolId, studentId)) {
      throw studentNotFound();
    }
  }

  private static long unresolvedCount(List<PsychologicalSignal> signals, SignalSeverity severity) {
    return signals.stream()
        .filter(signal -> signal.severity() == severity)
        .filter(signal -> !signal.resolved())
        .count();
  }

  private static ApiException studentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_STUDENT_NOT_FOUND", "O'quvchi topilmadi.", "ID ni tekshiring.");
  }

  private static ApiException signalNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_SIGNAL_NOT_FOUND", "Signal topilmadi.", "ID ni tekshiring.");
  }
}
