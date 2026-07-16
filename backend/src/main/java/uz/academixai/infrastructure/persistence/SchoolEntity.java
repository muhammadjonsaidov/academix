package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.School;

/**
 * JPA mapping for {@code schools} (backend_tdd.md §4.1). Full column set — expanded from a
 * read-only-lookup-only slice to support GET/PUT /admin/school (Sprint 14).
 */
@Entity
@Table(name = "schools")
public class SchoolEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String region;

  @Column(nullable = false)
  private String district;

  private String phone;

  private String email;

  @Column(name = "total_classes")
  private int totalClasses;

  @Column(name = "is_active")
  private boolean isActive;

  @Column(name = "subscribed_at")
  private LocalDateTime subscribedAt;

  @Column(name = "subscription_ends_at")
  private LocalDateTime subscriptionEndsAt;

  @Column(name = "admin_id")
  private UUID adminId;

  @Column(name = "monthly_ai_call_limit", nullable = false)
  private int monthlyAiCallLimit;

  @Column(name = "current_month_ai_usage", nullable = false)
  private int currentMonthAiUsage;

  protected SchoolEntity() {}

  public SchoolEntity(
      UUID id,
      String name,
      String address,
      String region,
      String district,
      String phone,
      String email,
      int totalClasses,
      boolean isActive,
      LocalDateTime subscribedAt,
      LocalDateTime subscriptionEndsAt,
      UUID adminId,
      int monthlyAiCallLimit,
      int currentMonthAiUsage) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.region = region;
    this.district = district;
    this.phone = phone;
    this.email = email;
    this.totalClasses = totalClasses;
    this.isActive = isActive;
    this.subscribedAt = subscribedAt;
    this.subscriptionEndsAt = subscriptionEndsAt;
    this.adminId = adminId;
    this.monthlyAiCallLimit = monthlyAiCallLimit;
    this.currentMonthAiUsage = currentMonthAiUsage;
  }

  public static SchoolEntity fromDomain(School domain) {
    return new SchoolEntity(
        domain.id(),
        domain.name(),
        domain.address(),
        domain.region(),
        domain.district(),
        domain.phone(),
        domain.email(),
        domain.totalClasses(),
        domain.isActive(),
        domain.subscribedAt(),
        domain.subscriptionEndsAt(),
        domain.adminId(),
        domain.monthlyAiCallLimit(),
        domain.currentMonthAiUsage());
  }

  public School toDomain() {
    return new School(
        id,
        name,
        address,
        region,
        district,
        phone,
        email,
        totalClasses,
        isActive,
        subscribedAt,
        subscriptionEndsAt,
        adminId,
        monthlyAiCallLimit,
        currentMonthAiUsage);
  }

  public UUID getId() {
    return id;
  }

  public UUID getAdminId() {
    return adminId;
  }

  public int getMonthlyAiCallLimit() {
    return monthlyAiCallLimit;
  }
}
