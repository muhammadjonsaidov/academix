package uz.academixai.reporting.application.port.out;

import java.util.UUID;
import uz.academixai.domain.SignalSeverity;

/** Outbound port for the unresolved psychological-alert counts shown on the admin dashboard. */
public interface WellbeingAlertCounts {

  int unresolved(UUID schoolId, SignalSeverity severity);
}
