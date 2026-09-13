package uz.academixai.progress.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.progress.domain.Badge;
import uz.academixai.progress.domain.BadgeCriteriaType;

/** JPA mapping for {@code badges} (migration V20). Maps to/from {@link Badge}. */
@Entity
@Table(name = "badges")
public class BadgeEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private String icon;

  @Enumerated(EnumType.STRING)
  @Column(name = "criteria_type", nullable = false)
  private BadgeCriteriaType criteriaType;

  @Column(name = "criteria_value", nullable = false)
  private int criteriaValue;

  protected BadgeEntity() {}

  public BadgeEntity(
      UUID id,
      String name,
      String description,
      String icon,
      BadgeCriteriaType criteriaType,
      int criteriaValue) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = icon;
    this.criteriaType = criteriaType;
    this.criteriaValue = criteriaValue;
  }

  public static BadgeEntity fromDomain(Badge domain) {
    return new BadgeEntity(
        domain.id(),
        domain.name(),
        domain.description(),
        domain.icon(),
        domain.criteriaType(),
        domain.criteriaValue());
  }

  public Badge toDomain() {
    return new Badge(id, name, description, icon, criteriaType, criteriaValue);
  }

  public UUID getId() {
    return id;
  }

  public BadgeCriteriaType getCriteriaType() {
    return criteriaType;
  }

  public int getCriteriaValue() {
    return criteriaValue;
  }
}
