package uz.academixai.wellbeing.application.port.out;

import java.util.UUID;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;

/** Notification boundary for the severity-based Wellbeing alert matrix. */
public interface PsychologicalAlertNotifier {

  boolean notifyParents(UUID studentId, SignalType type, SignalSeverity severity);

  void notifyTeacherAndPsychologists(UUID studentId, SignalType type, SignalSeverity severity);
}
