package uz.academixai.interfaces.web.psychologist;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.SignalWithStudent;

/** academix_tz.md §2.6 — GET /psychologist/signals, exact shape. */
public record PsychologistSignalListItemResponse(
    UUID signalId,
    String studentName,
    UUID studentId,
    String className,
    String type,
    String severity,
    String description,
    LocalDateTime detectedAt,
    boolean isManipulation) {

  public static PsychologistSignalListItemResponse from(SignalWithStudent item) {
    var signal = item.signal();
    return new PsychologistSignalListItemResponse(
        signal.id(),
        item.studentName(),
        signal.studentId(),
        item.className(),
        signal.type().name(),
        signal.severity().name(),
        signal.description(),
        signal.detectedAt(),
        signal.isManipulation());
  }
}
