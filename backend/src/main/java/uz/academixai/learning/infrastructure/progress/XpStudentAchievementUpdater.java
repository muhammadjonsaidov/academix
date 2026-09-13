package uz.academixai.learning.infrastructure.progress;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.learning.application.port.out.StudentAchievementUpdater;
import uz.academixai.progress.application.XPService;

/** Explicit Progress adapter for a final teacher grade. */
@Component
public class XpStudentAchievementUpdater implements StudentAchievementUpdater {

  private final XPService xp;

  public XpStudentAchievementUpdater(XPService xp) {
    this.xp = xp;
  }

  @Override
  public void applyGrade(
      UUID submissionId, UUID studentId, int score, boolean late, boolean excellent) {
    xp.calculateAndAwardXP(submissionId, score, late);
    xp.updateStreak(studentId, score);
    if (excellent) {
      xp.awardExcellentBonus(studentId);
    }
    xp.checkAndAwardBadges(studentId);
  }
}
