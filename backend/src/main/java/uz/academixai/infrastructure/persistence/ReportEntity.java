package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Report;
import uz.academixai.domain.ReportType;

/** JPA mapping for {@code reports} (V36 migration). Maps to/from {@link Report}. */
@Entity
@Table(name = "reports")
public class ReportEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportType type;

  @Column(nullable = false)
  private String quarter;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "file_url", nullable = false)
  private String fileUrl;

  @Column(name = "generated_by", nullable = false)
  private UUID generatedBy;

  @Column(name = "generated_at")
  private LocalDateTime generatedAt;

  protected ReportEntity() {}

  public ReportEntity(
      UUID id,
      UUID schoolId,
      ReportType type,
      String quarter,
      UUID targetId,
      String fileUrl,
      UUID generatedBy,
      LocalDateTime generatedAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.type = type;
    this.quarter = quarter;
    this.targetId = targetId;
    this.fileUrl = fileUrl;
    this.generatedBy = generatedBy;
    this.generatedAt = generatedAt;
  }

  public static ReportEntity fromDomain(Report domain) {
    return new ReportEntity(
        domain.id(),
        domain.schoolId(),
        domain.type(),
        domain.quarter(),
        domain.targetId(),
        domain.fileUrl(),
        domain.generatedBy(),
        domain.generatedAt());
  }

  public Report toDomain() {
    return new Report(id, schoolId, type, quarter, targetId, fileUrl, generatedBy, generatedAt);
  }
}
