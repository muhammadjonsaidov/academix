package uz.academixai.intelligence.application.port.out;

import java.util.UUID;
import uz.academixai.domain.SubmissionStatus;

/** Live-status notification boundary for a completed asynchronous analysis transition. */
public interface AnalysisStatusNotifier {

  void notifyHomework(UUID teacherId, UUID studentId, UUID submissionId, SubmissionStatus status);
}
