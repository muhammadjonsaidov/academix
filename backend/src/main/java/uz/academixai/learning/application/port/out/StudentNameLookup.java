package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Narrow Identity read used by the teacher review and analytics screens. */
public interface StudentNameLookup {

  /** Display name with a fallback, for screens that must always render something. */
  String fullName(UUID studentId);

  /**
   * First and last name separately, empty when there is no such user.
   *
   * <p>Separate from {@link #fullName} on purpose: teacher analytics returns the two fields
   * independently, and a caller that must answer 404 for a missing student cannot use a method
   * whose contract is to always return something.
   */
  Optional<Name> nameOf(UUID studentId);

  record Name(String firstName, String lastName) {}
}
