package uz.academixai.progress.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** Learning submission projection needed to maintain the XP delta for one submission. */
public interface HomeworkSubmissionXpStore {

  Optional<HomeworkSubmission> findById(UUID submissionId);

  HomeworkSubmission save(HomeworkSubmission submission);
}
