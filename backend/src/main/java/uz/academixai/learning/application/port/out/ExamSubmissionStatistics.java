package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Read model used when presenting a teacher's exam list. */
public interface ExamSubmissionStatistics {

  int countAll(UUID examId);

  int countGraded(UUID examId);
}
