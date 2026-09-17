package uz.academixai.school.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for resolving a class by the name written in an imported spreadsheet. */
public interface ClassByNameLookup {

  Optional<UUID> classIdOf(UUID schoolId, String fullName);
}
