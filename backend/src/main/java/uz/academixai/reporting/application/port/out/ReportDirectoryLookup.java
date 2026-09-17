package uz.academixai.reporting.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for the three display labels a report needs from outside Reporting — the school, a
 * class and a student.
 *
 * <p>Grouped into one port on purpose: they answer the same question ("what do we print for this
 * id?") for three different owners, and splitting them would add three interfaces without adding
 * meaning.
 */
public interface ReportDirectoryLookup {

  Optional<String> schoolName(UUID schoolId);

  /** The class' display label, e.g. "7-A". Empty when the class is not in that school. */
  Optional<String> classLabel(UUID classId, UUID schoolId);

  Optional<String> studentFullName(UUID studentUserId);
}
