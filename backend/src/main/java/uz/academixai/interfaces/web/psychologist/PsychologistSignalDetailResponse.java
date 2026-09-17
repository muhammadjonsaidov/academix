package uz.academixai.interfaces.web.psychologist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.BehaviorProfile;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.SignalDetail;

/**
 * academix_tz.md §2.6 — GET /psychologist/signals/{signalId}, exact shape. {@code
 * studentBehaviorProfile} deliberately never carries lesson/grade content — "DARS MAZMUNI YO'Q -
 * faqat xulq-atvor".
 */
public record PsychologistSignalDetailResponse(
    SignalPayload signal, BehaviorProfilePayload studentBehaviorProfile) {

  public record SignalPayload(
      UUID signalId,
      UUID studentId,
      String studentName,
      String className,
      String type,
      String severity,
      String description,
      boolean isManipulation,
      boolean resolved,
      LocalDateTime detectedAt,
      LocalDateTime resolvedAt,
      String resolutionNotes,
      String actionTaken) {}

  public record BehaviorProfilePayload(
      Map<String, Integer> activeHours,
      List<XpHistoryEntry> xpTrend,
      List<SubmissionDayPayload> submissionPattern,
      List<String> keyPhrases) {}

  public record SubmissionDayPayload(LocalDate date, int count) {}

  public static PsychologistSignalDetailResponse from(SignalDetail detail) {
    var item = detail.signal();
    var signal = item.signal();
    var payload =
        new SignalPayload(
            signal.id(),
            signal.studentId(),
            item.studentName(),
            item.className(),
            signal.type().name(),
            signal.severity().name(),
            signal.description(),
            signal.isManipulation(),
            signal.resolved(),
            signal.detectedAt(),
            signal.resolvedAt(),
            signal.resolutionNotes(),
            signal.actionTaken());
    BehaviorProfile profile = detail.behaviorProfile();
    var profilePayload =
        new BehaviorProfilePayload(
            profile.activeHours(),
            profile.xpTrend(),
            profile.submissionPattern().stream()
                .map(d -> new SubmissionDayPayload(d.date(), d.count()))
                .toList(),
            profile.keyPhrases());
    return new PsychologistSignalDetailResponse(payload, profilePayload);
  }
}
