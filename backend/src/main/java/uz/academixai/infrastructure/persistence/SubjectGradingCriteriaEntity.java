package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.CriteriaItem;
import uz.academixai.domain.SubjectGradingCriteria;

/**
 * JPA mapping for {@code subject_grading_criteria} (TZ §1.19, migration V17 — missing from
 * backend_tdd.md's DDL, see ROADMAP.md Sprint 3). Maps to/from {@link SubjectGradingCriteria}.
 */
@Entity
@Table(name = "subject_grading_criteria")
public class SubjectGradingCriteriaEntity {

  @Id private UUID id;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "criteria", nullable = false)
  private List<CriteriaItem> criteria;

  protected SubjectGradingCriteriaEntity() {}

  public SubjectGradingCriteriaEntity(
      UUID id, UUID subjectId, UUID teacherId, List<CriteriaItem> criteria) {
    this.id = id;
    this.subjectId = subjectId;
    this.teacherId = teacherId;
    this.criteria = criteria;
  }

  public static SubjectGradingCriteriaEntity fromDomain(SubjectGradingCriteria domain) {
    return new SubjectGradingCriteriaEntity(
        domain.id(), domain.subjectId(), domain.teacherId(), domain.criteria());
  }

  public SubjectGradingCriteria toDomain() {
    return new SubjectGradingCriteria(id, subjectId, teacherId, criteria);
  }

  public UUID getId() {
    return id;
  }
}
