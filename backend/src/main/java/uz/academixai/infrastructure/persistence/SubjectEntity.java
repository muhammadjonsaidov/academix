package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.domain.Subject;
import uz.academixai.domain.SubjectType;

/** JPA mapping for {@code subjects} (backend_tdd.md §4.1). Maps to/from {@link Subject}. */
@Entity
@Table(name = "subjects")
public class SubjectEntity {

  @Id private UUID id;

  @Column(name = "school_id")
  private UUID schoolId;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectType type;

  private String icon;

  protected SubjectEntity() {}

  public SubjectEntity(UUID id, UUID schoolId, String name, SubjectType type, String icon) {
    this.id = id;
    this.schoolId = schoolId;
    this.name = name;
    this.type = type;
    this.icon = icon;
  }

  public static SubjectEntity fromDomain(Subject domain) {
    return new SubjectEntity(
        domain.id(), domain.schoolId(), domain.name(), domain.type(), domain.icon());
  }

  public Subject toDomain() {
    return new Subject(id, schoolId, name, type, icon);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getName() {
    return name;
  }

  public SubjectType getType() {
    return type;
  }
}
