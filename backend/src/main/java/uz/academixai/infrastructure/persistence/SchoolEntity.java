package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.domain.School;

/**
 * JPA mapping for {@code schools} (backend_tdd.md §4.1). Intentionally partial — only the columns
 * needed for read-only lookups (e.g. resolving an admin's schoolId at login). Expand to the full
 * column set before building any School INSERT/UPDATE path (several columns are NOT NULL in the
 * migration and this entity would fail on insert as-is).
 */
@Entity
@Table(name = "schools")
public class SchoolEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "admin_id")
  private UUID adminId;

  @Column(name = "monthly_ai_call_limit", nullable = false)
  private int monthlyAiCallLimit;

  protected SchoolEntity() {}

  public School toDomain() {
    return new School(id, name, adminId, monthlyAiCallLimit);
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
