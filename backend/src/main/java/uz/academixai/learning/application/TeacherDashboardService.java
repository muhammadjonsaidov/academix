package uz.academixai.learning.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.learning.application.port.out.HomeworkSubmissionQuery;
import uz.academixai.learning.application.port.out.TeacherGradingStatistics;
import uz.academixai.school.application.port.in.TeacherAccess;

/**
 * academix_tz.md §2.3 {@code GET /teacher/dashboard} — exact response shape.
 *
 * <p>Moved here from the legacy {@code application} package, and to Learning rather than Reporting:
 * every figure is Learning's own data (submissions awaiting a grade, grades recorded, the teacher's
 * rating), and the only outside fact is the class list, which School already publishes through
 * {@link TeacherAccess}. Reporting owns the school-wide comparisons an admin sees; this is the
 * teacher's view of their own work, and they will diverge.
 */
@Service
public class TeacherDashboardService {

  private static final int CLASS_PROGRESS_WINDOW_DAYS = 30;
  private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

  private final TeacherAccess teacherAccess;
  private final HomeworkSubmissionQuery submissions;
  private final TeacherGradingStatistics grades;

  public TeacherDashboardService(
      TeacherAccess teacherAccess,
      HomeworkSubmissionQuery submissions,
      TeacherGradingStatistics grades) {
    this.teacherAccess = teacherAccess;
    this.submissions = submissions;
    this.grades = grades;
  }

  public record Rating(double score, String trend) {}

  public record Dashboard(
      List<SchoolClass> myClasses,
      int pendingSubmissions,
      long gradedToday,
      Rating myRating,
      List<TeacherGradingStatistics.ClassProgress> classProgressSummary) {}

  public Dashboard dashboard(UUID schoolId, UUID teacherId) {
    List<SchoolClass> myClasses = teacherAccess.myClasses(schoolId, teacherId);
    Set<UUID> classIds = myClasses.stream().map(SchoolClass::id).collect(Collectors.toSet());

    int pendingSubmissions = submissions.countPendingGradeByTeacher(schoolId, teacherId);
    long gradedToday = grades.gradedSince(teacherId, LocalDate.now().atStartOfDay());

    Rating myRating = buildRating(teacherId);

    List<TeacherGradingStatistics.ClassProgress> classProgressSummary =
        grades
            .classProgress(schoolId, LocalDateTime.now().minusDays(CLASS_PROGRESS_WINDOW_DAYS))
            .stream()
            .filter(row -> classIds.contains(row.classId()))
            .toList();

    return new Dashboard(
        myClasses, pendingSubmissions, gradedToday, myRating, classProgressSummary);
  }

  // "myRating" isn't defined beyond the {score: 4.7, trend: "+0.2"} example (same judgment call as
  // the admin teacher ranking) — score is this teacher's all-time average five_point_grade, trend
  // compares this calendar month's average against last month's.
  private Rating buildRating(UUID teacherId) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

    TeacherGradingStatistics.Rating allTime = grades.rating(teacherId, EPOCH, now);
    TeacherGradingStatistics.Rating thisMonth = grades.rating(teacherId, thisMonthStart, now);
    TeacherGradingStatistics.Rating lastMonth =
        grades.rating(teacherId, lastMonthStart, thisMonthStart);

    double diff = thisMonth.avgGrade() - lastMonth.avgGrade();
    String trend =
        lastMonth.gradedCount() == 0
            ? "N/A"
            : (diff >= 0 ? "+" : "") + Math.round(diff * 10) / 10.0;

    return new Rating(Math.round(allTime.avgGrade() * 10) / 10.0, trend);
  }
}
