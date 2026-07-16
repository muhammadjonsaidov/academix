package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.domain.SubmissionStatus;

public interface ExamSubmissionRepository extends JpaRepository<ExamSubmissionEntity, UUID> {

  List<ExamSubmissionEntity> findByExamIdOrderByUploadedAtDesc(UUID examId);

  List<ExamSubmissionEntity> findByStudentIdOrderByUploadedAtDesc(UUID studentId);

  Optional<ExamSubmissionEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  Optional<ExamSubmissionEntity> findByIdAndExamId(UUID id, UUID examId);

  int countByExamId(UUID examId);

  int countByExamIdAndStatus(UUID examId, SubmissionStatus status);
}
