package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;

/** JPA mapping for {@code users} (backend_tdd.md §4.1). Maps to/from {@link User}. */
@Entity
@Table(name = "users")
public class UserEntity {

  @Id private UUID id;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String phone;

  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // Not part of the domain User record (academix_tz.md §1.1 deliberately has no schoolId on
  // User) — added via V6 migration solely to scope TEACHER accounts to a school. Null for
  // every other role. See V6__add_school_id_to_users.sql and CLAUDE.md "Reality checks".
  @Column(name = "school_id")
  private UUID schoolId;

  protected UserEntity() {}

  public UserEntity(
      UUID id,
      String firstName,
      String lastName,
      String phone,
      String email,
      String passwordHash,
      Role role,
      boolean isActive,
      LocalDateTime createdAt,
      LocalDateTime lastLoginAt) {
    this(
        id,
        firstName,
        lastName,
        phone,
        email,
        passwordHash,
        role,
        isActive,
        createdAt,
        lastLoginAt,
        null);
  }

  public UserEntity(
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
      UUID schoolId) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.phone = phone;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.lastLoginAt = lastLoginAt;
    this.schoolId = schoolId;
  }

  public static UserEntity fromDomain(User user) {
    return new UserEntity(
        user.id(),
        user.firstName(),
        user.lastName(),
        user.phone(),
        user.email(),
        user.passwordHash(),
        user.role(),
        user.isActive(),
        user.createdAt(),
        user.lastLoginAt());
  }

  public User toDomain() {
    return new User(
        id,
        firstName,
        lastName,
        phone,
        email,
        passwordHash,
        role,
        isActive,
        createdAt,
        lastLoginAt);
  }

  public UUID getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public boolean isActive() {
    return isActive;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public UUID getSchoolId() {
    return schoolId;
  }
}
