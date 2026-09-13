package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassSubjectStatsRow;
import uz.academixai.infrastructure.persistence.GradeRepository.StudentProgressRow;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.progress.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;
import uz.academixai.school.application.port.in.TeacherAccess;

/**
 * academix_tz.md §2.3 "O'quvchi progressi"/"Sinf taqqoslash" — both scoped to "faqat o'z sinfi"
 * (teacher's own class only). No topic-level tracking exists anywhere in this codebase, so
 * weak/strong-area granularity falls back to subject-level (see {@link
 * GradeRepository#classSubjectStats}'s Javadoc) — a real, flagged simplification, not the literal
 * per-topic breakdown the spec's field names suggest.
 */
@Service
public class TeacherAnalyticsService {

  private static final int PROGRESS_WINDOW_DAYS = 90;
  private static final int RECENT_SUBMISSIONS_LIMIT = 10;
  private static final int TOP_BOTTOM_LIMIT = 5;

  private final TeacherAccess teacherAccess;
  private final StudentProfileRepository studentProfileRepository;
  private final UserRepository userRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final XpHistoryRepository xpHistoryRepository;
  private final GradeRepository gradeRepository;
  private final SchoolClassRepository classRepository;

  public TeacherAnalyticsService(
      TeacherAccess teacherAccess,
      StudentProfileRepository studentProfileRepository,
      UserRepository userRepository,
      HomeworkSubmissionRepository submissionRepository,
      XpHistoryRepository xpHistoryRepository,
      GradeRepository gradeRepository,
      SchoolClassRepository classRepository) {
    this.teacherAccess = teacherAccess;
    this.studentProfileRepository = studentProfileRepository;
    this.userRepository = userRepository;
    this.submissionRepository = submissionRepository;
    this.xpHistoryRepository = xpHistoryRepository;
    this.gradeRepository = gradeRepository;
    this.classRepository = classRepository;
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
    StudentProfile profile =
        studentProfileRepository
            .findByUserIdAndSchoolId(studentId, schoolId)
            .map(StudentProfileEntity::toDomain)
            .orElseThrow(TeacherAnalyticsService::studentNotFound);
    // Side-effecting authorization check — throws ERR_NOT_ASSIGNED if this teacher isn't
    // assigned to the student's class ("faqat o'z sinfi").
    teacherAccess.requireAssignedToClass(schoolId, teacherId, profile.classId());

    UserEntity user =
        userRepository.findById(studentId).orElseThrow(TeacherAnalyticsService::studentNotFound);

    LocalDateTime since = LocalDateTime.now().minusDays(PROGRESS_WINDOW_DAYS);
    List<SubjectStat> subjectStats =
        buildSubjectStats(schoolId, profile.classId(), studentId, since);

    List<XpHistoryEntry> xpHistory =
        xpHistoryRepository.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
            .map(XpHistoryEntity::toDomain)
            .toList();

    List<HomeworkSubmission> recentSubmissions =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
            .limit(RECENT_SUBMISSIONS_LIMIT)
            .map(HomeworkSubmissionEntity::toDomain)
            .toList();

    return new StudentProgress(
        studentId,
        user.getFirstName(),
        user.getLastName(),
        subjectStats,
        xpHistory,
        recentSubmissions);
  }

  public record ClassAnalytics(
      double classAverage,
      List<StudentProgressRow> topStudents,
      List<StudentProgressRow> bottomStudents,
      List<String> subjectWeakAreas,
      List<SubjectStat> submissionRateBySubject) {}

  public ClassAnalytics classAnalytics(UUID schoolId, UUID teacherId, UUID classId) {
    teacherAccess.requireAssignedToClass(schoolId, teacherId, classId);

    SchoolClass schoolClass =
        classRepository
            .findByIdAndSchoolId(classId, schoolId)
            .map(SchoolClassEntity::toDomain)
            .orElseThrow(TeacherAnalyticsService::classNotFound);

    LocalDateTime since = LocalDateTime.now().minusDays(PROGRESS_WINDOW_DAYS);
    List<StudentProgressRow> allStudents =
        gradeRepository.classStudentProgress(schoolId, classId, since).stream()
            .filter(row -> row.getGradedCount() > 0)
            .sorted((a, b) -> Double.compare(b.getAvgScore(), a.getAvgScore()))
            .toList();
    List<StudentProgressRow> topStudents = allStudents.stream().limit(TOP_BOTTOM_LIMIT).toList();
    List<StudentProgressRow> bottomStudents =
        allStudents.reversed().stream().limit(TOP_BOTTOM_LIMIT).toList();

    double classAverage =
        allStudents.stream().mapToDouble(StudentProgressRow::getAvgScore).average().orElse(0.0);

    List<ClassSubjectStatsRow> subjectRows =
        gradeRepository.classSubjectStats(schoolId, classId, null, since, LocalDateTime.now());
    List<String> subjectWeakAreas =
        subjectRows.stream()
            .filter(row -> row.getGradedCount() > 0)
            .sorted((a, b) -> Double.compare(a.getAvgScore(), b.getAvgScore()))
            .limit(3)
            .map(ClassSubjectStatsRow::getSubjectName)
            .toList();
    List<SubjectStat> submissionRateBySubject =
        subjectRows.stream().map(row -> toSubjectStat(row, schoolClass.studentCount())).toList();

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
    List<ClassSubjectStatsRow> current =
        gradeRepository.classSubjectStats(schoolId, classId, studentId, since, now);

    LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
    List<ClassSubjectStatsRow> thisMonth =
        gradeRepository.classSubjectStats(schoolId, classId, studentId, thisMonthStart, now);
    List<ClassSubjectStatsRow> lastMonth =
        gradeRepository.classSubjectStats(
            schoolId, classId, studentId, lastMonthStart, thisMonthStart);

    return current.stream()
        .map(
            row -> {
              double assigned = row.getAssignedCount();
              double submissionRate =
                  assigned == 0 ? 0 : (row.getSubmittedCount() / assigned) * 100;
              String trend = trendFor(row.getSubjectName(), thisMonth, lastMonth);
              return new SubjectStat(
                  row.getSubjectName(),
                  Math.round(row.getAvgScore() * 10) / 10.0,
                  Math.round(submissionRate * 10) / 10.0,
                  trend);
            })
        .toList();
  }

  private SubjectStat toSubjectStat(ClassSubjectStatsRow row, int classSize) {
    double assigned = row.getAssignedCount();
    double submissionRate =
        assigned == 0 || classSize == 0
            ? 0
            : (row.getSubmittedCount() / (assigned * classSize)) * 100;
    return new SubjectStat(
        row.getSubjectName(),
        Math.round(row.getAvgScore() * 10) / 10.0,
        Math.round(Math.min(submissionRate, 100) * 10) / 10.0,
        "");
  }

  // Real month-over-month % change per subject, this-month vs last-month average score.
  private static String trendFor(
      String subjectName,
      List<ClassSubjectStatsRow> thisMonth,
      List<ClassSubjectStatsRow> lastMonth) {
    double thisAvg =
        thisMonth.stream()
            .filter(r -> r.getSubjectName().equals(subjectName))
            .mapToDouble(ClassSubjectStatsRow::getAvgScore)
            .findFirst()
            .orElse(0);
    double lastAvg =
        lastMonth.stream()
            .filter(r -> r.getSubjectName().equals(subjectName))
            .mapToDouble(ClassSubjectStatsRow::getAvgScore)
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
