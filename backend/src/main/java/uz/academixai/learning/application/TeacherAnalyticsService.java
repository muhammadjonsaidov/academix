package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.learning.application.port.out.ClassStudentCount;
import uz.academixai.learning.application.port.out.ClassSubjectStatistics;
import uz.academixai.learning.application.port.out.StudentClassLookup;
import uz.academixai.learning.application.port.out.StudentNameLookup;
import uz.academixai.progress.application.port.in.StudentXpHistory;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.school.application.port.in.TeacherAccess;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §2.3 "O'quvchi progressi"/"Sinf taqqoslash" — both scoped to "faqat o'z sinfi"
 * (teacher's own class only). No topic-level tracking exists anywhere in this codebase, so
 * weak/strong-area granularity falls back to subject-level — a real, flagged simplification, not
 * the literal per-topic breakdown the spec's field names suggest.
 *
 * <p>Moved here from the legacy {@code application} package, and it is the first cross-context move
 * that needed a prerequisite: it read {@code progress}' XP table directly, through another
 * context's infrastructure. It now uses Progress' published {@code StudentXpHistory} port — which
 * turned out to run the exact same query, so the blocker was a wiring question rather than an API
 * gap. Five of its dependencies were already ports; only the two grade-statistics reads needed new
 * ones.
 */
@Service
public class TeacherAnalyticsService {

  private static final int PROGRESS_WINDOW_DAYS = 90;
  private static final int RECENT_SUBMISSIONS_LIMIT = 10;
  private static final int TOP_BOTTOM_LIMIT = 5;

  private final TeacherAccess teacherAccess;
  private final StudentClassLookup studentClasses;
  private final StudentNameLookup studentNames;
  private final ClassSubjectStatistics gradeStatistics;
  private final ClassStudentCount classStudentCounts;
  private final StudentXpHistory xpHistory;
  private final uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository
      submissions;

  public TeacherAnalyticsService(
      TeacherAccess teacherAccess,
      StudentClassLookup studentClasses,
      StudentNameLookup studentNames,
      ClassSubjectStatistics gradeStatistics,
      ClassStudentCount classStudentCounts,
      StudentXpHistory xpHistory,
      uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository submissions) {
    this.teacherAccess = teacherAccess;
    this.studentClasses = studentClasses;
    this.studentNames = studentNames;
    this.gradeStatistics = gradeStatistics;
    this.classStudentCounts = classStudentCounts;
    this.xpHistory = xpHistory;
    this.submissions = submissions;
  }

  public record SubjectStat(
      String subject, double averageScore, double submissionRate, String trend) {}

  public record StudentProgress(
      UUID studentId,
      String firstName,
      String lastName,
      List<SubjectStat> subjectStats,
      List<XpHistoryEntry> xpHistory,
      List<HomeworkSubmission> recentSubmissions) {}

  public StudentProgress studentProgress(UUID schoolId, UUID teacherId, UUID studentId) {
    UUID classId =
        studentClasses
            .classId(schoolId, studentId)
            .orElseThrow(TeacherAnalyticsService::studentNotFound);
    // Side-effecting authorization check — throws ERR_NOT_ASSIGNED if this teacher isn't
    // assigned to the student's class ("faqat o'z sinfi").
    teacherAccess.requireAssignedToClass(schoolId, teacherId, classId);

    StudentNameLookup.Name name =
        studentNames.nameOf(studentId).orElseThrow(TeacherAnalyticsService::studentNotFound);

    LocalDateTime since = LocalDateTime.now().minusDays(PROGRESS_WINDOW_DAYS);
    List<SubjectStat> subjectStats = buildSubjectStats(schoolId, classId, studentId, since);

    List<XpHistoryEntry> xp = xpHistory.recentOf(studentId);
    List<HomeworkSubmission> recentSubmissions =
        submissions.findByStudentId(studentId).stream().limit(RECENT_SUBMISSIONS_LIMIT).toList();

    return new StudentProgress(
        studentId, name.firstName(), name.lastName(), subjectStats, xp, recentSubmissions);
  }

  public record ClassAnalytics(
      double classAverage,
      List<ClassSubjectStatistics.StudentRow> topStudents,
      List<ClassSubjectStatistics.StudentRow> bottomStudents,
      List<String> subjectWeakAreas,
      List<SubjectStat> submissionRateBySubject) {}

  public ClassAnalytics classAnalytics(UUID schoolId, UUID teacherId, UUID classId) {
    teacherAccess.requireAssignedToClass(schoolId, teacherId, classId);

    int classSize =
        classStudentCounts
            .of(classId, schoolId)
            .orElseThrow(TeacherAnalyticsService::classNotFound);

    LocalDateTime since = LocalDateTime.now().minusDays(PROGRESS_WINDOW_DAYS);
    List<ClassSubjectStatistics.StudentRow> allStudents =
        gradeStatistics.classStudentProgress(schoolId, classId, since).stream()
            .filter(row -> row.gradedCount() > 0)
            .sorted((a, b) -> Double.compare(b.avgScore(), a.avgScore()))
            .toList();
    List<ClassSubjectStatistics.StudentRow> topStudents =
        allStudents.stream().limit(TOP_BOTTOM_LIMIT).toList();
    List<ClassSubjectStatistics.StudentRow> bottomStudents =
        allStudents.reversed().stream().limit(TOP_BOTTOM_LIMIT).toList();

    double classAverage =
        allStudents.stream()
            .mapToDouble(ClassSubjectStatistics.StudentRow::avgScore)
            .average()
            .orElse(0.0);

    List<ClassSubjectStatistics.SubjectRow> subjectRows =
        gradeStatistics.classSubjectStats(schoolId, classId, null, since, LocalDateTime.now());
    List<String> subjectWeakAreas =
        subjectRows.stream()
            .filter(row -> row.gradedCount() > 0)
            .sorted((a, b) -> Double.compare(a.avgScore(), b.avgScore()))
            .limit(3)
            .map(ClassSubjectStatistics.SubjectRow::subjectName)
            .toList();
    List<SubjectStat> submissionRateBySubject =
        subjectRows.stream().map(row -> toSubjectStat(row, classSize)).toList();

    return new ClassAnalytics(
        Math.round(classAverage * 10) / 10.0,
        topStudents,
        bottomStudents,
        subjectWeakAreas,
        submissionRateBySubject);
  }

  private List<SubjectStat> buildSubjectStats(
      UUID schoolId, UUID classId, UUID studentId, LocalDateTime since) {
    LocalDateTime now = LocalDateTime.now();
    List<ClassSubjectStatistics.SubjectRow> current =
        gradeStatistics.classSubjectStats(schoolId, classId, studentId, since, now);

    LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
    List<ClassSubjectStatistics.SubjectRow> thisMonth =
        gradeStatistics.classSubjectStats(schoolId, classId, studentId, thisMonthStart, now);
    List<ClassSubjectStatistics.SubjectRow> lastMonth =
        gradeStatistics.classSubjectStats(
            schoolId, classId, studentId, lastMonthStart, thisMonthStart);

    return current.stream()
        .map(
            row -> {
              double assigned = row.assignedCount();
              double submissionRate = assigned == 0 ? 0 : (row.submittedCount() / assigned) * 100;
              String trend = trendFor(row.subjectName(), thisMonth, lastMonth);
              return new SubjectStat(
                  row.subjectName(),
                  Math.round(row.avgScore() * 10) / 10.0,
                  Math.round(submissionRate * 10) / 10.0,
                  trend);
            })
        .toList();
  }

  private SubjectStat toSubjectStat(ClassSubjectStatistics.SubjectRow row, int classSize) {
    double assigned = row.assignedCount();
    double submissionRate =
        assigned == 0 || classSize == 0 ? 0 : (row.submittedCount() / (assigned * classSize)) * 100;
    return new SubjectStat(
        row.subjectName(),
        Math.round(row.avgScore() * 10) / 10.0,
        Math.round(Math.min(submissionRate, 100) * 10) / 10.0,
        "");
  }

  // Real month-over-month % change per subject, this-month vs last-month average score.
  private static String trendFor(
      String subjectName,
      List<ClassSubjectStatistics.SubjectRow> thisMonth,
      List<ClassSubjectStatistics.SubjectRow> lastMonth) {
    double thisAvg =
        thisMonth.stream()
            .filter(r -> r.subjectName().equals(subjectName))
            .mapToDouble(ClassSubjectStatistics.SubjectRow::avgScore)
            .findFirst()
            .orElse(0);
    double lastAvg =
        lastMonth.stream()
            .filter(r -> r.subjectName().equals(subjectName))
            .mapToDouble(ClassSubjectStatistics.SubjectRow::avgScore)
            .findFirst()
            .orElse(0);
    if (lastAvg == 0) {
      return "N/A";
    }
    double changePercent = ((thisAvg - lastAvg) / lastAvg) * 100;
    return (changePercent >= 0 ? "+" : "") + Math.round(changePercent) + "%";
  }

  private static ApiException studentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_STUDENT_NOT_FOUND", "O'quvchi topilmadi.", "ID ni tekshiring.");
  }

  private static ApiException classNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_CLASS_NOT_FOUND", "Sinf topilmadi.", "ID ni tekshiring.");
  }
}
