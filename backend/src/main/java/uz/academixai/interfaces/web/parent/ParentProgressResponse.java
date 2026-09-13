package uz.academixai.interfaces.web.parent;

import java.util.List;
import uz.academixai.application.ParentProgressService.MonthlyXp;
import uz.academixai.application.ParentProgressService.Progress;
import uz.academixai.application.ParentProgressService.SubjectProgress;
import uz.academixai.progress.application.port.in.StudentDashboard.DashboardBadge;

/** academix_tz.md §2.5 — GET /parent/children/{studentId}/progress, exact top-level fields. */
public record ParentProgressResponse(
    List<SubjectProgress> subjectProgress, List<MonthlyXp> monthlyXpChart, List<BadgeItem> badges) {

  public record BadgeItem(String name, String icon, String awardedAt) {}

  public static ParentProgressResponse from(Progress progress) {
    return new ParentProgressResponse(
        progress.subjectProgress(),
        progress.monthlyXpChart(),
        progress.badges().stream().map(ParentProgressResponse::toBadgeItem).toList());
  }

  private static BadgeItem toBadgeItem(DashboardBadge b) {
    return new BadgeItem(b.badge().name(), b.badge().icon(), b.awardedAt().toString());
  }
}
