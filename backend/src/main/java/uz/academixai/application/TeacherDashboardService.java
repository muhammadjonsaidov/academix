package uz.academixai.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.TeacherRatingRow;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.school.application.port.in.TeacherAccess;

/** academix_tz.md §2.3 {@code GET /teacher/dashboard} — exact response shape. */
@Service
public class TeacherDashboardService {

  private static final int CLASS_PROGRESS_WINDOW_DAYS = 30;
  private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

  private final TeacherAccess teacherAccess;
  private final HomeworkSubmissionRepository submissionRepository;
  private final GradeRepository gradeRepository;

  public TeacherDashboardService(
      TeacherAccess teacherAccess,
      HomeworkSubmissionRepository submissionRepository,
      GradeRepository gradeRepository) {
    this.teacherAccess = teacherAccess;
    this.submissionRepository = submissionRepository;
    this.gradeRepository = gradeRepository;
  }

  public record Rating(double score, String trend) {}

  public record Dashboard(
      List<SchoolClass> myClasses,
      int pendingSubmissions,
      long gradedToday,
      Rating myRating,
      List<ClassProgressRow> classProgressSummary) {}

  public Dashboard dashboard(UUID schoolId, UUID teacherId) {
    List<SchoolClass> myClasses = teacherAccess.myClasses(schoolId, teacherId);
    Set<UUID> classIds = myClasses.stream().map(SchoolClass::id).collect(Collectors.toSet());

    int pendingSubmissions = submissionRepository.countPendingGradeByTeacher(schoolId, teacherId);
    long gradedToday =
        gradeRepository.countByTeacherIdAndGradedAtAfter(teacherId, LocalDate.now().atStartOfDay());

    Rating myRating = buildRating(teacherId);

    List<ClassProgressRow> classProgressSummary =
        gradeRepository
            .classProgress(
                schoolId, null, LocalDateTime.now().minusDays(CLASS_PROGRESS_WINDOW_DAYS))
            .stream()
            .filter(row -> classIds.contains(row.getClassId()))
            .toList();

    return new Dashboard(
        myClasses, pendingSubmissions, gradedToday, myRating, classProgressSummary);
  }

  // "myRating" isn't defined beyond the {score: 4.7, trend: "+0.2"} example (same judgment call
  // as AdminAnalyticsService's teacher ranking) — score is this teacher's all-time average
  // five_point_grade, trend compares this calendar month's average against last month's.
  private Rating buildRating(UUID teacherId) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

    TeacherRatingRow allTime = gradeRepository.teacherRatingForWindow(teacherId, EPOCH, now);
    TeacherRatingRow thisMonth =
        gradeRepository.teacherRatingForWindow(teacherId, thisMonthStart, now);
    TeacherRatingRow lastMonth =
        gradeRepository.teacherRatingForWindow(teacherId, lastMonthStart, thisMonthStart);

    double diff = thisMonth.getAvgGrade() - lastMonth.getAvgGrade();
    String trend =
        lastMonth.getGradedCount() == 0
            ? "N/A"
            : (diff >= 0 ? "+" : "") + Math.round(diff * 10) / 10.0;

    return new Rating(Math.round(allTime.getAvgGrade() * 10) / 10.0, trend);
  }
}
