package uz.academixai.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;

/** Identity aggregate used for authentication and account self-service operations. */
public record Account(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    String email,
    String passwordHash,
    Role role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    UUID schoolId) {

  public Account withLastLoginAt(LocalDateTime value) {
    return new Account(
        id,
        firstName,
        lastName,
        phone,
        email,
        passwordHash,
        role,
        active,
        createdAt,
        value,
        schoolId);
  }

  public Account withProfile(String firstName, String lastName, String email) {
    return new Account(
        id,
        firstName,
        lastName,
        phone,
        email,
        passwordHash,
        role,
        active,
        createdAt,
        lastLoginAt,
        schoolId);
  }

  public Account withPasswordHash(String passwordHash) {
    return new Account(
        id,
        firstName,
        lastName,
        phone,
        email,
        passwordHash,
        role,
        active,
        createdAt,
        lastLoginAt,
        schoolId);
  }

  public User toUser() {
    return new User(
        id, firstName, lastName, phone, email, passwordHash, role, active, createdAt, lastLoginAt);
  }
}
