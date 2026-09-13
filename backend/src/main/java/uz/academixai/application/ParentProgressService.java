package uz.academixai.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.application.StudentDashboardService.DashboardBadge;
import uz.academixai.application.StudentSubmissionService.StudentHomeworkItem;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.progress.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;

/**
 * academix_tz.md §2.5 — {@code GET /parent/children/{studentId}/progress}, {@code .../homework},
 * {@code .../submissions}, {@code .../grades}. {@code subjectProgress}'s exact fields are given
 * ({@code subject, currentAvg, previousMonthAvg, growth, submissionRate, trend}) but the
 * computation itself isn't — judgment call: current vs previous *calendar* month, {@code trend}
 * thresholded at ±2 points (STABLE otherwise). Class average / other students are never computed or
 * exposed anywhere in this service, matching §2.5's explicit rule.
 */
@Service
public class ParentProgressService {

  private static final double TREND_THRESHOLD = 2.0;
  private static final int RECENT_GRADES_LIMIT = 10;
  private static final int XP_CHART_MONTHS = 6;

  private final ParentLinkService parentLinkService;
  private final StudentProfileRepository studentProfileRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final HomeworkAssignmentRepository assignmentRepository;
  private final SubjectRepository subjectRepository;
  private final GradeRepository gradeRepository;
  private final AIFeedbackRepository feedbackRepository;
  private final XpHistoryRepository xpHistoryRepository;
  private final StudentSubmissionService studentSubmissionService;
  private final StudentDashboardService studentDashboardService;

  public ParentProgressService(
      ParentLinkService parentLinkService,
      StudentProfileRepository studentProfileRepository,
      HomeworkSubmissionRepository submissionRepository,
      HomeworkAssignmentRepository assignmentRepository,
      SubjectRepository subjectRepository,
      GradeRepository gradeRepository,
      AIFeedbackRepository feedbackRepository,
      XpHistoryRepository xpHistoryRepository,
      StudentSubmissionService studentSubmissionService,
      StudentDashboardService studentDashboardService) {
    this.parentLinkService = parentLinkService;
    this.studentProfileRepository = studentProfileRepository;
    this.submissionRepository = submissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.subjectRepository = subjectRepository;
    this.gradeRepository = gradeRepository;
    this.feedbackRepository = feedbackRepository;
    this.xpHistoryRepository = xpHistoryRepository;
    this.studentSubmissionService = studentSubmissionService;
    this.studentDashboardService = studentDashboardService;
  }

  public record SubjectProgress(
      String subject,
      double currentAvg,
      double previousMonthAvg,
      double growth,
      double submissionRate,
      String trend) {}

  public record MonthlyXp(String month, int xp) {}

  public record Progress(
      List<SubjectProgress> subjectProgress,
      List<MonthlyXp> monthlyXpChart,
      List<DashboardBadge> badges) {}

  public Progress progress(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    return computeProgress(studentId);
  }

  /**
   * Sprint 14 — {@code GET /student/progress} reuses the exact same subject-progress/monthly-XP
   * computation, just without the parent-link authorization gate (a student always owns their own
   * data).
   */
  public Progress progressForStudent(UUID studentId) {
    return computeProgress(studentId);
  }

  private Progress computeProgress(UUID studentId) {
    StudentProfileEntity profile = studentProfileRepository.findByUserId(studentId).orElseThrow();
    UUID schoolId = profile.getSchoolId();

    List<HomeworkSubmissionEntity> submissions =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
    YearMonth currentMonth = YearMonth.now();
    YearMonth previousMonth = currentMonth.minusMonths(1);

    Map<UUID, List<ScoredSubmission>> bySubject = new LinkedHashMap<>();
    for (HomeworkSubmissionEntity sub : submissions) {
      HomeworkSubmission domain = sub.toDomain();
      HomeworkAssignmentEntity assignment =
          assignmentRepository.findById(domain.assignmentId()).orElse(null);
      if (assignment == null) {
        continue;
      }
      Double score = resolveScore(domain);
      bySubject
          .computeIfAbsent(assignment.getSubjectId(), k -> new ArrayList<>())
          .add(new ScoredSubmission(domain.submittedAt().toLocalDate(), score));
    }

    List<SubjectProgress> subjectProgress = new ArrayList<>();
    for (Map.Entry<UUID, List<ScoredSubmission>> entry : bySubject.entrySet()) {
      String subjectName =
          subjectRepository.findById(entry.getKey()).map(SubjectEntity::getName).orElse("Fan");
      List<ScoredSubmission> items = entry.getValue();
      double currentAvg = averageForMonth(items, currentMonth);
      double previousAvg = averageForMonth(items, previousMonth);
      double growth = currentAvg - previousAvg;
      long assignedThisMonth =
          assignmentRepository
              .findBySchoolIdAndClassIdOrderByDeadlineAtDesc(schoolId, profile.getClassId())
              .stream()
              .filter(a -> a.getSubjectId().equals(entry.getKey()))
              .filter(a -> YearMonth.from(a.toDomain().assignedAt()).equals(currentMonth))
              .count();
      long submittedThisMonth =
          items.stream().filter(i -> YearMonth.from(i.date()).equals(currentMonth)).count();
      double submissionRate =
          assignedThisMonth == 0 ? 0 : (double) submittedThisMonth / assignedThisMonth;
      String trend =
          growth > TREND_THRESHOLD ? "UP" : growth < -TREND_THRESHOLD ? "DOWN" : "STABLE";
      subjectProgress.add(
          new SubjectProgress(subjectName, currentAvg, previousAvg, growth, submissionRate, trend));
    }

    List<MonthlyXp> monthlyXpChart = buildMonthlyXpChart(studentId);
    List<DashboardBadge> badges = studentDashboardService.listBadges(schoolId, studentId);

    return new Progress(subjectProgress, monthlyXpChart, badges);
  }

  public List<StudentHomeworkItem> homework(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    UUID schoolId =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::getSchoolId)
            .orElseThrow();
    return studentSubmissionService.listHomework(schoolId, studentId);
  }

  public List<HomeworkSubmission> submissions(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    UUID schoolId =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::getSchoolId)
            .orElseThrow();
    return studentSubmissionService.listSubmissions(schoolId, studentId);
  }

  public record GradeItem(UUID submissionId, String subject, int score, int fivePointGrade) {}

  public record Grades(List<GradeItem> recent, Map<String, Double> bySubject) {}

  public Grades grades(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    List<HomeworkSubmissionEntity> submissions =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);

    List<GradeItem> recent = new ArrayList<>();
    Map<String, List<Integer>> scoresBySubject = new LinkedHashMap<>();
    for (HomeworkSubmissionEntity sub : submissions) {
      if (sub.toDomain().status() != SubmissionStatus.GRADED) {
        continue;
      }
      GradeEntity grade = gradeRepository.findBySubmissionId(sub.getId()).orElse(null);
      if (grade == null) {
        continue;
      }
      HomeworkAssignmentEntity assignment =
          assignmentRepository.findById(sub.toDomain().assignmentId()).orElse(null);
      String subjectName =
          assignment == null
              ? "Fan"
              : subjectRepository
                  .findById(assignment.getSubjectId())
                  .map(SubjectEntity::getName)
                  .orElse("Fan");
      var gradeDomain = grade.toDomain();
      if (recent.size() < RECENT_GRADES_LIMIT) {
        recent.add(
            new GradeItem(
                sub.getId(), subjectName, gradeDomain.score(), gradeDomain.fivePointGrade()));
      }
      scoresBySubject.computeIfAbsent(subjectName, k -> new ArrayList<>()).add(gradeDomain.score());
    }

    Map<String, Double> bySubject = new LinkedHashMap<>();
    scoresBySubject.forEach(
        (subject, scores) ->
            bySubject.put(
                subject, scores.stream().mapToInt(Integer::intValue).average().orElse(0)));

    return new Grades(recent, bySubject);
  }

  private Double resolveScore(HomeworkSubmission submission) {
    if (submission.status() == SubmissionStatus.GRADED) {
      return gradeRepository
          .findBySubmissionId(submission.id())
          .map(g -> (double) g.toDomain().score())
          .orElse(null);
    }
    return feedbackRepository
        .findBySubmissionId(submission.id())
        .map(AIFeedbackEntity::toDomain)
        .map(f -> (double) f.aiScorePercent())
        .orElse(null);
  }

  private static double averageForMonth(List<ScoredSubmission> items, YearMonth month) {
    return items.stream()
        .filter(i -> YearMonth.from(i.date()).equals(month))
        .map(ScoredSubmission::score)
        .filter(Optional::isPresent)
        .mapToDouble(Optional::get)
        .average()
        .orElse(0);
  }

  private List<MonthlyXp> buildMonthlyXpChart(UUID studentId) {
    List<XpHistoryEntity> history =
        xpHistoryRepository.findByStudentIdOrderByOccurredAtDesc(studentId);
    Map<YearMonth, Integer> byMonth = new LinkedHashMap<>();
    YearMonth start = YearMonth.now().minusMonths(XP_CHART_MONTHS - 1L);
    for (int i = 0; i < XP_CHART_MONTHS; i++) {
      byMonth.put(start.plusMonths(i), 0);
    }
    for (XpHistoryEntity entry : history) {
      YearMonth month = YearMonth.from(entry.toDomain().occurredAt());
      if (byMonth.containsKey(month)) {
        byMonth.merge(month, entry.toDomain().xp(), Integer::sum);
      }
    }
    List<MonthlyXp> chart = new ArrayList<>();
    byMonth.forEach((month, xp) -> chart.add(new MonthlyXp(month.toString(), xp)));
    return chart;
  }

  private record ScoredSubmission(LocalDate date, Optional<Double> score) {
    ScoredSubmission(LocalDate date, Double score) {
      this(date, Optional.ofNullable(score));
    }
  }
}
