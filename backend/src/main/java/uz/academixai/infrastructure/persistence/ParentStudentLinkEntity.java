package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.ParentRelation;
import uz.academixai.domain.ParentStudentLink;

/** JPA mapping for {@code parent_student_links} (backend_tdd.md §4.1 table 6). */
@Entity
@Table(name = "parent_student_links")
public class ParentStudentLinkEntity {

  @Id private UUID id;

  @Column(name = "parent_user_id", nullable = false)
  private UUID parentUserId;

  @Column(name = "student_user_id", nullable = false)
  private UUID studentUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ParentRelation relation;

  @Column(name = "is_active")
  private boolean isActive;

  @Column(name = "biometric_consent_given", nullable = false)
  private boolean biometricConsentGiven;

  @Column(name = "consent_given_at")
  private LocalDateTime consentGivenAt;

  protected ParentStudentLinkEntity() {}

  public ParentStudentLinkEntity(
      UUID id,
      UUID parentUserId,
      UUID studentUserId,
      ParentRelation relation,
      boolean isActive,
      boolean biometricConsentGiven,
      LocalDateTime consentGivenAt) {
    this.id = id;
    this.parentUserId = parentUserId;
    this.studentUserId = studentUserId;
    this.relation = relation;
    this.isActive = isActive;
    this.biometricConsentGiven = biometricConsentGiven;
    this.consentGivenAt = consentGivenAt;
  }

  public static ParentStudentLinkEntity fromDomain(ParentStudentLink domain) {
    return new ParentStudentLinkEntity(
        domain.id(),
        domain.parentUserId(),
        domain.studentUserId(),
        domain.relation(),
        domain.isActive(),
        domain.biometricConsentGiven(),
        domain.consentGivenAt());
  }

  public ParentStudentLink toDomain() {
    return new ParentStudentLink(
        id, parentUserId, studentUserId, relation, isActive, biometricConsentGiven, consentGivenAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getParentUserId() {
    return parentUserId;
  }

  public UUID getStudentUserId() {
    return studentUserId;
  }
}
