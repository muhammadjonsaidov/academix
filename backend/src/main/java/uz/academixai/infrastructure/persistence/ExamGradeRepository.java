package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamGradeRepository extends JpaRepository<ExamGradeEntity, UUID> {

  Optional<ExamGradeEntity> findByExamSubmissionId(UUID examSubmissionId);
}
