package uz.academixai.progress.infrastructure.legacy;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.ParentProgressService;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.application.port.in.ParentProgress;
import uz.academixai.progress.application.port.in.StudentDashboard.DashboardBadge;

/**
 * Explicit temporary adapter while Parent Progress persistence moves into the Progress context.
 *
 * <p>The web layer depends only on the published Progress API now; no caller needs to know the
 * legacy service during the remaining internal migration.
 */
@Component
public class LegacyParentProgress implements ParentProgress {

  private final ParentProgressService legacy;

  public LegacyParentProgress(ParentProgressService legacy) {
    this.legacy = legacy;
  }

  @Override
  public Progress progress(UUID parentUserId, UUID studentId) {
    ParentProgressService.Progress value = legacy.progress(parentUserId, studentId);
    List<SubjectProgress> subjects =
        value.subjectProgress().stream()
            .map(
                item ->
                    new SubjectProgress(
                        item.subject(),
                        item.currentAvg(),
                        item.previousMonthAvg(),
                        item.growth(),
                        item.submissionRate(),
                        item.trend()))
            .toList();
    List<MonthlyXp> chart =
        value.monthlyXpChart().stream()
            .map(item -> new MonthlyXp(item.month(), item.xp()))
            .toList();
    List<DashboardBadge> badges = value.badges();
    return new Progress(subjects, chart, badges);
  }

  @Override
  public List<HomeworkItem> homework(UUID parentUserId, UUID studentId) {
    return legacy.homework(parentUserId, studentId);
  }

  @Override
  public List<uz.academixai.domain.HomeworkSubmission> submissions(
      UUID parentUserId, UUID studentId) {
    return legacy.submissions(parentUserId, studentId);
  }

  @Override
  public Grades grades(UUID parentUserId, UUID studentId) {
    ParentProgressService.Grades value = legacy.grades(parentUserId, studentId);
    return new Grades(
        value.recent().stream()
            .map(
                item ->
                    new GradeItem(
                        item.submissionId(), item.subject(), item.score(), item.fivePointGrade()))
            .toList(),
        value.bySubject());
  }
}
