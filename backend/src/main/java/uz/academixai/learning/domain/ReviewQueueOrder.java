package uz.academixai.learning.domain;

import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.PlagiarismType;

/**
 * Order of work waiting for the teacher: anything the AI flagged as copied or machine-written comes
 * first (it needs a human look), then the oldest submissions, so nobody waits longest.
 */
public final class ReviewQueueOrder {

  private ReviewQueueOrder() {}

  public static boolean needsCloseLook(AIFeedback feedback) {
    return feedback != null
        && feedback.plagiarismType() != null
        && feedback.plagiarismType() != PlagiarismType.CLEAN;
  }
}
