package uz.academixai.school.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Role;

/**
 * Account operations School needs for the people it manages: students, teachers and psychologists.
 *
 * <p>The account row belongs to Identity; School reads and writes the school membership part of it
 * (role + {@code school_id}) and nothing else. This port keeps that dependency from becoming direct
 * {@code users}-table access — see {@code Family}'s {@code ParentAccountStore}, which is the same
 * shape for parents. Both are transitional: once Identity owns account management, one published
 * account API replaces them.
 *
 * <p>Writes go through the 11-argument {@code UserEntity} constructor in the adapter so {@code
 * school_id} survives a rebuild; the 10-argument form silently nulls it (see CLAUDE.md).
 */
public interface MemberAccountStore {

  Optional<MemberAccount> findInSchool(UUID id, Role role, UUID schoolId);

  boolean existsByPhone(String phone);

  List<MemberAccount> findByRoleInSchool(Role role, UUID schoolId);

  MemberAccount save(MemberAccount account);

  String encodePassword(String rawPassword);

  String generateTemporaryPassword();

  /** The membership-relevant view of an account, as School sees it. */
  record MemberAccount(
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
