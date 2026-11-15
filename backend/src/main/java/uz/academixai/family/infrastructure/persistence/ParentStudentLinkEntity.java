package uz.academixai.family.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.family.domain.ParentRelation;
import uz.academixai.family.domain.ParentStudentLink;

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

  protected ParentStudentLinkEntity() {}

  public ParentStudentLinkEntity(
      UUID id, UUID parentUserId, UUID studentUserId, ParentRelation relation, boolean isActive) {
    this.id = id;
    this.parentUserId = parentUserId;
    this.studentUserId = studentUserId;
    this.relation = relation;
    this.isActive = isActive;
  }

  public static ParentStudentLinkEntity fromDomain(ParentStudentLink domain) {
    return new ParentStudentLinkEntity(
        domain.id(),
        domain.parentUserId(),
        domain.studentUserId(),
        domain.relation(),
        domain.isActive());
  }

  public ParentStudentLink toDomain() {
    return new ParentStudentLink(id, parentUserId, studentUserId, relation, isActive);
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
