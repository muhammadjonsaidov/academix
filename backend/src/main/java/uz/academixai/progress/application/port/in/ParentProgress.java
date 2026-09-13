package uz.academixai.progress.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;

/** Published Progress API for a parent's authorized view of one child's learning progress. */
public interface ParentProgress {

  record SubjectProgress(
      String subject,
      double currentAvg,
      double previousMonthAvg,
      double growth,
      double submissionRate,
      String trend) {}

  record MonthlyXp(String month, int xp) {}

  record Progress(
      List<SubjectProgress> subjectProgress,
      List<MonthlyXp> monthlyXpChart,
      List<StudentDashboard.DashboardBadge> badges) {}

  record GradeItem(UUID submissionId, String subject, int score, int fivePointGrade) {}

  record Grades(List<GradeItem> recent, Map<String, Double> bySubject) {}

  Progress progress(UUID parentUserId, UUID studentId);

  List<HomeworkItem> homework(UUID parentUserId, UUID studentId);

  List<HomeworkSubmission> submissions(UUID parentUserId, UUID studentId);

  Grades grades(UUID parentUserId, UUID studentId);
}
