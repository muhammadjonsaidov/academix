package uz.academixai.intelligence.application.port.out;

import java.util.UUID;

/** Published Progress capability used until SubmissionGraded events replace this adapter. */
public interface ProgressAchievementAwarder {

  void applyAiScore(UUID submissionId, UUID studentId, float score, boolean late);
}
