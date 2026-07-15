package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.ImportColumnMapping;

/**
 * JPA mapping for {@code import_column_mappings} (backend_tdd.md §4.1 table 19). Maps to/from
 * {@link ImportColumnMapping}.
 */
@Entity
@Table(name = "import_column_mappings")
public class ImportColumnMappingEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false, unique = true)
  private UUID schoolId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private Map<String, String> mapping;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected ImportColumnMappingEntity() {}

  public ImportColumnMappingEntity(
      UUID id, UUID schoolId, Map<String, String> mapping, LocalDateTime updatedAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.mapping = mapping;
    this.updatedAt = updatedAt;
  }

  public static ImportColumnMappingEntity fromDomain(ImportColumnMapping domain) {
    return new ImportColumnMappingEntity(
        domain.id(), domain.schoolId(), domain.mapping(), domain.updatedAt());
  }

  public ImportColumnMapping toDomain() {
    return new ImportColumnMapping(id, schoolId, mapping, updatedAt);
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public Map<String, String> getMapping() {
    return mapping;
  }
}
