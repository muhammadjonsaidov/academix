package uz.academixai.intelligence.infrastructure.progress;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.port.out.ProgressAchievementAwarder;
import uz.academixai.progress.application.XPService;

/**
 * Transitional synchronous Progress adapter; replaced by SubmissionGraded outbox consumption later.
 */
@Component
public class XpProgressAchievementAwarder implements ProgressAchievementAwarder {

  private final XPService xp;

  public XpProgressAchievementAwarder(XPService xp) {
    this.xp = xp;
  }

  @Override
  public void applyAiScore(UUID submissionId, UUID studentId, float score, boolean late) {
    xp.calculateAndAwardXP(submissionId, score, late);
    xp.updateStreak(studentId, score);
    xp.checkAndAwardBadges(studentId);
  }
}
