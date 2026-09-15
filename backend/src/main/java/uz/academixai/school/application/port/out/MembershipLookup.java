package uz.academixai.school.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * The three membership facts the school lookup needs, each owned by a different table: a school
 * names its admin, an account carries {@code school_id} for teachers and psychologists, and a
 * student's profile carries it for students.
 */
public interface MembershipLookup {

  Optional<UUID> schoolOfAdmin(UUID adminUserId);

  /** {@code users.school_id} — the V6 deviation column. */
  Optional<UUID> schoolOfAccount(UUID userId);

  Optional<UUID> schoolOfStudent(UUID studentUserId);
}
