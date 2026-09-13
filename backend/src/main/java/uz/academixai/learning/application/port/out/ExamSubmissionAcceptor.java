package uz.academixai.learning.application.port.out;

import uz.academixai.domain.ExamSubmission;

/**
 * Commits one accepted paper and its durable AI-processing event as an independent transaction.
 * This preserves partial success for a large scanned-paper batch.
 */
public interface ExamSubmissionAcceptor {

  void accept(ExamSubmission submission);
}
