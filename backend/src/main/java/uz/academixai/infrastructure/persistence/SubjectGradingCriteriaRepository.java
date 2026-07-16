package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectGradingCriteriaRepository
    extends JpaRepository<SubjectGradingCriteriaEntity, UUID> {

  Optional<SubjectGradingCriteriaEntity> findBySubjectIdAndTeacherId(
      UUID subjectId, UUID teacherId);
}
