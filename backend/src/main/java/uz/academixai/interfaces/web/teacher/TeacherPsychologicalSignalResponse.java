package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.wellbeing.application.TeacherPsychologyService.SignalWithStudent;

/**
 * academix_tz.md §2.3 — response shape inferred (spec gives paths only), see
 * TeacherPsychologyService's Javadoc. No {@code isManipulation} field — psychologist-only per
 * §1.14.
 */
public record TeacherPsychologicalSignalResponse(
    UUID signalId,
    UUID studentId,
    String studentName,
    String type,
    String severity,
    String description,
    boolean resolved,
    LocalDateTime detectedAt,
    LocalDateTime resolvedAt) {

  public static TeacherPsychologicalSignalResponse from(SignalWithStudent item) {
    var signal = item.signal();
    return new TeacherPsychologicalSignalResponse(
        signal.id(),
        signal.studentId(),
        item.studentName(),
        signal.type().name(),
        signal.severity().name(),
        signal.description(),
        signal.resolved(),
        signal.detectedAt(),
        signal.resolvedAt());
  }
}
