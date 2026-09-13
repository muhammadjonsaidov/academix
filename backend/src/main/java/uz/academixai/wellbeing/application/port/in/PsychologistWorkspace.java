package uz.academixai.wellbeing.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.WatchlistEntry;
import uz.academixai.progress.domain.XpHistoryEntry;

/** Published psychologist-facing Wellbeing workspace. */
public interface PsychologistWorkspace {

  record Dashboard(
      long criticalSignals,
      long highSignals,
      long mediumSignals,
      long resolvedThisWeek,
      List<WatchlistEntryWithStudent> watchlistStudents) {}

  record SignalWithStudent(PsychologicalSignal signal, String studentName, String className) {}

  record BehaviorProfile(
      Map<String, Integer> activeHours,
      List<XpHistoryEntry> xpTrend,
      List<SubmissionDay> submissionPattern,
      List<String> keyPhrases) {}

  record SubmissionDay(LocalDate date, int count) {}

  record SignalDetail(SignalWithStudent signal, BehaviorProfile behaviorProfile) {}

  record WatchlistEntryWithStudent(WatchlistEntry entry, String studentName) {}

  record MonthlyReport(
      long totalSignals,
      Map<String, Long> bySeverity,
      Map<String, Long> byType,
      long resolvedCount,
      long manipulationFlaggedCount) {}

  Dashboard dashboard(UUID schoolId);

  List<SignalWithStudent> listSignals(UUID schoolId, SignalSeverity severity, Boolean resolved);

  SignalDetail getSignalDetail(UUID schoolId, UUID signalId);

  PsychologicalSignal resolve(UUID schoolId, UUID signalId, String notes, String actionTaken);

  PsychologicalSignal markManipulation(UUID schoolId, UUID signalId);

  List<WatchlistEntryWithStudent> watchlist(UUID schoolId);

  WatchlistEntry addToWatchlist(UUID schoolId, UUID psychologistId, UUID studentId, String reason);

  void removeFromWatchlist(UUID schoolId, UUID studentId);

  MonthlyReport monthlyReport(UUID schoolId);
}
