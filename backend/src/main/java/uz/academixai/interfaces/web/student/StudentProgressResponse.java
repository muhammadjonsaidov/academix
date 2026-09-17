package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.progress.application.StudentProgressService.Progress;

/** academix_tz.md §2.4 {@code GET /student/progress} — exact response shape. */
public record StudentProgressResponse(
    List<XpHistoryResponse> xpHistory,
    List<SubjectProgressResponse> subjectStats,
    List<BadgeResponse> badges,
    List<String> streakHistory,
    MyGrowthResponse myGrowth) {

  public record GrowthResponse(double avgScore) {}

  public record MyGrowthResponse(
      GrowthResponse thisMonth, GrowthResponse lastMonth, String growth) {}

  public static StudentProgressResponse from(Progress progress) {
    return new StudentProgressResponse(
        progress.xpHistory().stream().map(XpHistoryResponse::from).toList(),
        progress.subjectStats().stream().map(SubjectProgressResponse::from).toList(),
        progress.badges().stream().map(BadgeResponse::from).toList(),
        progress.streakHistory(),
        new MyGrowthResponse(
            new GrowthResponse(progress.myGrowth().thisMonth().avgScore()),
            new GrowthResponse(progress.myGrowth().lastMonth().avgScore()),
            progress.myGrowth().growth()));
  }
}
