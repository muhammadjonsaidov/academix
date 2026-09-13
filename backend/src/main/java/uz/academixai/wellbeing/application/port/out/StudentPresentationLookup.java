package uz.academixai.wellbeing.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Minimal student display data needed by psychologist read models. */
public interface StudentPresentationLookup {

  record StudentPresentation(String fullName, String className) {}

  Optional<StudentPresentation> find(UUID studentId);
}
