package uz.academixai.progress.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.application.port.in.ParentProgress;
import uz.academixai.progress.application.port.in.StudentDashboard;
import uz.academixai.progress.application.port.out.ParentChildAccess;
import uz.academixai.progress.application.port.out.ParentProgressStore;

/** Progress-owned calculation of a linked child's grades, completion trends and XP history. */
@Service
public class ParentProgressService implements ParentProgress {

  private static final double TREND_THRESHOLD = 2.0;
  private static final int RECENT_GRADES_LIMIT = 10;
  private static final int XP_CHART_MONTHS = 6;

  private final ParentChildAccess access;
  private final ParentProgressStore store;
  private final StudentHomeworkQuery homework;
  private final StudentDashboard dashboard;

  public ParentProgressService(
      ParentChildAccess access,
      ParentProgressStore store,
      StudentHomeworkQuery homework,
      StudentDashboard dashboard) {
    this.access = access;
    this.store = store;
    this.homework = homework;
    this.dashboard = dashboard;
  }

  @Override
  public Progress progress(UUID parentUserId, UUID studentId) {
    access.requireLinkedChild(parentUserId, studentId);
    return progressForStudent(studentId);
  }

  /** Reuses the same calculation for a student viewing their own subject statistics. */
  public Progress progressForStudent(UUID studentId) {
    var profile = store.findStudentProfile(studentId).orElseThrow();
    YearMonth current = YearMonth.now();
    YearMonth previous = current.minusMonths(1);
    Map<UUID, List<ScoredSubmission>> bySubject = new LinkedHashMap<>();
    for (HomeworkSubmission submission : store.findSubmissions(studentId)) {
      HomeworkAssignment assignment = store.findAssignment(submission.assignmentId()).orElse(null);
      if (assignment != null) {
        bySubject
            .computeIfAbsent(assignment.subjectId(), ignored -> new ArrayList<>())
            .add(new ScoredSubmission(submission.submittedAt().toLocalDate(), score(submission)));
      }
    }
    List<SubjectProgress> subjects = new ArrayList<>();
    for (var entry : bySubject.entrySet()) {
      List<ScoredSubmission> scores = entry.getValue();
      double currentAvg = average(scores, current);
      double previousAvg = average(scores, previous);
      long assigned =
          store.findAssignments(profile.schoolId(), profile.classId()).stream()
              .filter(assignment -> assignment.subjectId().equals(entry.getKey()))
              .filter(assignment -> YearMonth.from(assignment.assignedAt()).equals(current))
              .count();
      long submitted =
          scores.stream().filter(score -> YearMonth.from(score.date()).equals(current)).count();
      double rate = assigned == 0 ? 0 : (double) submitted / assigned;
      double growth = currentAvg - previousAvg;
      subjects.add(
          new SubjectProgress(
              store.subjectName(entry.getKey()),
              currentAvg,
              previousAvg,
              growth,
              rate,
              growth > TREND_THRESHOLD ? "UP" : growth < -TREND_THRESHOLD ? "DOWN" : "STABLE"));
    }
    return new Progress(
        subjects, monthlyXp(studentId), dashboard.listBadges(profile.schoolId(), studentId));
  }

  @Override
  public List<HomeworkItem> homework(UUID parentUserId, UUID studentId) {
    access.requireLinkedChild(parentUserId, studentId);
    return homework.listHomework(
        store.findStudentProfile(studentId).orElseThrow().schoolId(), studentId);
  }

  @Override
  public List<HomeworkSubmission> submissions(UUID parentUserId, UUID studentId) {
    access.requireLinkedChild(parentUserId, studentId);
    return homework.listSubmissions(
        store.findStudentProfile(studentId).orElseThrow().schoolId(), studentId);
  }

  @Override
  public Grades grades(UUID parentUserId, UUID studentId) {
    access.requireLinkedChild(parentUserId, studentId);
    List<GradeItem> recent = new ArrayList<>();
    Map<String, List<Integer>> scores = new LinkedHashMap<>();
    for (HomeworkSubmission submission : store.findSubmissions(studentId)) {
      if (submission.status() != SubmissionStatus.GRADED) continue;
      var grade = store.findGrade(submission.id()).orElse(null);
      if (grade == null) continue;
      String subject =
          store
              .findAssignment(submission.assignmentId())
              .map(a -> store.subjectName(a.subjectId()))
              .orElse("Fan");
      if (recent.size() < RECENT_GRADES_LIMIT)
        recent.add(new GradeItem(submission.id(), subject, grade.score(), grade.fivePointGrade()));
      scores.computeIfAbsent(subject, ignored -> new ArrayList<>()).add(grade.score());
    }
    Map<String, Double> averages = new LinkedHashMap<>();
    scores.forEach(
        (subject, values) ->
            averages.put(subject, values.stream().mapToInt(Integer::intValue).average().orElse(0)));
    return new Grades(recent, averages);
  }

  private Double score(HomeworkSubmission submission) {
    if (submission.status() == SubmissionStatus.GRADED)
      return store.findGrade(submission.id()).map(grade -> (double) grade.score()).orElse(null);
    return store
        .findAiFeedback(submission.id())
        .map(feedback -> (double) feedback.aiScorePercent())
        .orElse(null);
  }

  private List<MonthlyXp> monthlyXp(UUID studentId) {
    Map<YearMonth, Integer> values = new LinkedHashMap<>();
    YearMonth start = YearMonth.now().minusMonths(XP_CHART_MONTHS - 1L);
    for (int index = 0; index < XP_CHART_MONTHS; index++) values.put(start.plusMonths(index), 0);
    store
        .findXpHistory(studentId)
        .forEach(
            entry -> {
              YearMonth month = YearMonth.from(entry.occurredAt());
              if (values.containsKey(month)) values.merge(month, entry.xp(), Integer::sum);
            });
    return values.entrySet().stream()
        .map(entry -> new MonthlyXp(entry.getKey().toString(), entry.getValue()))
        .toList();
  }

  private static double average(List<ScoredSubmission> items, YearMonth month) {
    return items.stream()
        .filter(item -> YearMonth.from(item.date()).equals(month))
        .map(ScoredSubmission::score)
        .filter(Optional::isPresent)
        .mapToDouble(Optional::get)
        .average()
        .orElse(0);
  }

  private record ScoredSubmission(LocalDate date, Optional<Double> score) {
    ScoredSubmission(LocalDate date, Double score) {
      this(date, Optional.ofNullable(score));
    }
  }
}
