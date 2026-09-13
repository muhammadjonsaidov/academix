package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Progress context API invoked once a teacher confirms a final score. */
public interface StudentAchievementUpdater {

  void applyGrade(UUID submissionId, UUID studentId, int score, boolean late, boolean excellent);
}
