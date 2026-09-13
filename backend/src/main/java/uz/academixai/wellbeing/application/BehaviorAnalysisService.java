package uz.academixai.wellbeing.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.wellbeing.application.port.in.BehaviorAnalysis;
import uz.academixai.wellbeing.application.port.out.ActiveStudentDirectory;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;
import uz.academixai.wellbeing.application.port.out.BehaviorAnalyzer;
import uz.academixai.wellbeing.application.port.out.EvidencePayloadSerializer;
import uz.academixai.wellbeing.application.port.out.PsychologicalAlertNotifier;
import uz.academixai.wellbeing.application.port.out.PsychologicalSignalStore;
import uz.academixai.wellbeing.domain.SignalCandidate;

/**
 * Wellbeing-owned nightly behavioral analysis and severity-based alert policy.
 *
 * <p>The use case receives only activity metadata and student-originated chat text. It never reads
 * grades or homework content, and it treats provider output as untrusted candidates.
 */
@Service
public class BehaviorAnalysisService implements BehaviorAnalysis {

  private static final Logger log = LoggerFactory.getLogger(BehaviorAnalysisService.class);
  private static final int LOOKBACK_DAYS = 14;
  private static final int MAX_CHAT_MESSAGES = 20;
  private static final LocalTime NIGHT_START = LocalTime.of(23, 0);
  private static final LocalTime NIGHT_END = LocalTime.of(5, 0);

  private final ActiveStudentDirectory students;
  private final BehaviorActivityLookup activity;
  private final BehaviorAnalyzer analyzer;
  private final PsychologicalSignalStore signals;
  private final PsychologicalAlertNotifier notifications;
  private final EvidencePayloadSerializer evidenceSerializer;

  public BehaviorAnalysisService(
      ActiveStudentDirectory students,
      BehaviorActivityLookup activity,
      BehaviorAnalyzer analyzer,
      PsychologicalSignalStore signals,
      PsychologicalAlertNotifier notifications,
      EvidencePayloadSerializer evidenceSerializer) {
    this.students = students;
    this.activity = activity;
    this.analyzer = analyzer;
    this.signals = signals;
    this.notifications = notifications;
    this.evidenceSerializer = evidenceSerializer;
  }

  @Override
  public int analyzeAllActiveStudents() {
    int totalSignals = 0;
    List<ActiveStudentDirectory.Student> activeStudents = students.findAllActive();
    for (ActiveStudentDirectory.Student student : activeStudents) {
      totalSignals += analyze(student.studentId(), student.schoolId()).size();
    }
    log.info(
        "Nightly psychology analysis: {} active students, {} signals created",
        activeStudents.size(),
        totalSignals);
    return totalSignals;
  }

  @Override
  public List<PsychologicalSignal> analyzeStudent(UUID studentId) {
    return students
        .findSchoolId(studentId)
        .map(schoolId -> analyze(studentId, schoolId))
        .orElseGet(
            () -> {
              log.warn(
                  "Psychological analysis skipped for student {}: no school profile.", studentId);
              return List.of();
            });
  }

  private List<PsychologicalSignal> analyze(UUID studentId, UUID schoolId) {
    LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
    BehaviorActivityLookup.Activity activitySnapshot =
        activity.find(schoolId, studentId, since, MAX_CHAT_MESSAGES);
    uz.academixai.wellbeing.domain.BehaviorAnalysis result;
    try {
      result = analyzer.analyze(toPrompt(activitySnapshot));
    } catch (BehaviorAnalysisUnavailableException exception) {
      log.warn("Psychology analysis unavailable for student {}", studentId, exception);
      return List.of();
    }

    List<PsychologicalSignal> created = new ArrayList<>();
    for (SignalCandidate candidate : result.signals()) {
      SignalType type = parseType(candidate.type());
      SignalSeverity severity = parseSeverity(candidate.severity());
      if (type == null || severity == null) {
        log.warn(
            "Ignoring invalid psychology provider candidate type={} severity={}",
            candidate.type(),
            candidate.severity());
        continue;
      }
      created.add(
          createSignal(
              studentId,
              type,
              severity,
              candidate.evidence(),
              result.manipulationSuspected() && type == SignalType.MANIPULATION_ATTEMPT));
    }
    return created;
  }

  private PsychologicalSignal createSignal(
      UUID studentId,
      SignalType type,
      SignalSeverity severity,
      String evidence,
      boolean manipulation) {
    boolean notifyStaff = severity != SignalSeverity.LOW;
    boolean parentNotified =
        severity == SignalSeverity.CRITICAL
            && notifications.notifyParents(studentId, type, severity);
    PsychologicalSignal saved =
        signals.save(
            new PsychologicalSignal(
                UUID.randomUUID(),
                studentId,
                type,
                severity,
                evidence,
                evidenceSerializer.serialize(evidence),
                manipulation,
                notifyStaff,
                parentNotified,
                notifyStaff,
                false,
                LocalDateTime.now(),
                null,
                null,
                null));
    if (notifyStaff) {
      notifications.notifyTeacherAndPsychologists(studentId, type, severity);
    }
    if (severity == SignalSeverity.CRITICAL && !parentNotified) {
      log.warn(
          "CRITICAL psychological signal for student {} has no linked parent to notify.",
          studentId);
    }
    return saved;
  }

  private static String toPrompt(BehaviorActivityLookup.Activity snapshot) {
    long nightSubmissions =
        snapshot.submissionTimes().stream().filter(BehaviorAnalysisService::isNightTime).count();
    long daysSinceLastSubmission =
        snapshot.lastSubmissionDate() == null
            ? LOOKBACK_DAYS
            : ChronoUnit.DAYS.between(snapshot.lastSubmissionDate(), LocalDate.now());
    String chatSummary =
        snapshot.recentChatMessages().isEmpty()
            ? "AI chat yozishmalari: shu davrda yo'q."
            : "AI chat orqali yuborilgan xabarlar:\n- "
                + String.join("\n- ", snapshot.recentChatMessages());
    return """
        Oxirgi %d kunlik faollik: jami %d marta topshiriq yubordi, shulardan %d marotaba \
        tungi soat 23:00-05:00 oralig'ida. Oxirgi topshiriqdan beri %d kun o'tdi. \
        Shu davrda jami %d XP to'pladi.

        %s"""
        .formatted(
            LOOKBACK_DAYS,
            snapshot.submissionTimes().size(),
            nightSubmissions,
            daysSinceLastSubmission,
            snapshot.recentXp(),
            chatSummary);
  }

  private static boolean isNightTime(LocalDateTime dateTime) {
    LocalTime time = dateTime.toLocalTime();
    return time.isAfter(NIGHT_START) || time.isBefore(NIGHT_END);
  }

  private static SignalType parseType(String raw) {
    try {
      return SignalType.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return null;
    }
  }

  private static SignalSeverity parseSeverity(String raw) {
    try {
      return SignalSeverity.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return null;
    }
  }
}
