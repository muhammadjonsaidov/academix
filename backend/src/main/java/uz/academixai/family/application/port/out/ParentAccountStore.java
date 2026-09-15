package uz.academixai.family.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Role;

/**
 * Account operations Family needs for the parent account itself.
 *
 * <p>The account row belongs to Identity; Family only ever reads or creates a PARENT account, and
 * this port keeps that dependency from turning into direct {@code users}-table access. It exists
 * because {@code users.school_id} (the V6 deviation column) is what makes a school-scoped parent
 * list possible, and rebuilding a {@code UserEntity} is how that column gets preserved.
 */
public interface ParentAccountStore {

  Optional<ParentAccount> findByPhone(String phone);

  boolean existsByPhone(String phone);

  /** Persists a parent account, preserving {@code school_id} on updates. */
  ParentAccount save(ParentAccount account);

  List<ParentAccount> findParentsOfSchool(UUID schoolId);

  String encodePassword(String rawPassword);

  String generateTemporaryPassword();

  /**
   * The parent account as Family sees it. {@code passwordHash} and {@code schoolId} are carried
   * explicitly: the first because the caller hashes before creating, the second because dropping it
   * silently un-scopes the parent (see the V6 note).
   */
  record ParentAccount(
      UUID id,
      String firstName,
      String lastName,
      String phone,
      String email,
      String passwordHash,
      Role role,
      boolean isActive,
      LocalDateTime createdAt,
      LocalDateTime lastLoginAt,
      UUID schoolId) {}
}
