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

  // Not findByExamIdAndStudentId — no DB constraint stops a class teacher re-running bulk-upload
  // for the same exam (e.g. correcting a bad scan), so more than one row can legitimately exist;
  // a plain findBy would throw IncorrectResultSizeDataAccessException the moment that happens
  // (confirmed by a real 500 the first time this got exercised twice against the same exam).
  Optional<ExamSubmissionEntity> findFirstByExamIdAndStudentIdOrderByUploadedAtDesc(
      UUID examId, UUID studentId);

  int countByExamId(UUID examId);

  int countByExamIdAndStatus(UUID examId, SubmissionStatus status);
}
