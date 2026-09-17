package uz.academixai.progress.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.progress.application.port.out.HomeworkSubmissionXpStore;

/** Transitional Learning projection adapter used only for XP delta bookkeeping. */
@Repository
public class JpaHomeworkSubmissionXpStore implements HomeworkSubmissionXpStore {

  private final HomeworkSubmissionRepository submissions;

  public JpaHomeworkSubmissionXpStore(HomeworkSubmissionRepository submissions) {
    this.submissions = submissions;
  }

  @Override
  public Optional<HomeworkSubmission> findById(UUID submissionId) {
    return submissions.findById(submissionId).map(HomeworkSubmissionEntity::toDomain);
  }

  @Override
  public HomeworkSubmission save(HomeworkSubmission submission) {
    return submissions.save(HomeworkSubmissionEntity.fromDomain(submission)).toDomain();
  }
}
