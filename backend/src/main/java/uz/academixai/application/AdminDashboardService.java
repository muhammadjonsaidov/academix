package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.TeacherRankingRow;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.persistence.UserRepository;

/**
 * academix_tz.md §2.2 {@code GET /admin/dashboard} — exact response shape. {@code activeToday} and
 * {@code homeworkSubmissionRate} both use submission activity as the "active" proxy (see {@code
 * HomeworkSubmissionRepository.countDistinctStudentsSubmittedSince}'s Javadoc for why); {@code
 * classProgressList}/{@code teacherRankings} are scored over the last 30 days of grades, a judgment
 * call since the spec gives no window for the dashboard's own summary lists (the dedicated {@code
 * /admin/analytics/*} endpoints take an explicit {@code period}).
 */
@Service
public class AdminDashboardService {

  private static final int SUBMISSION_RATE_WINDOW_DAYS = 30;
  private static final int DASHBOARD_SCORE_WINDOW_DAYS = 30;

  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository schoolClassRepository;
  private final SubjectRepository subjectRepository;
  private final ClassSubjectTeacherRepository assignmentRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final GradeRepository gradeRepository;
  private final PsychologicalSignalRepository signalRepository;

  public AdminDashboardService(
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository schoolClassRepository,
      SubjectRepository subjectRepository,
      ClassSubjectTeacherRepository assignmentRepository,
      HomeworkSubmissionRepository submissionRepository,
      GradeRepository gradeRepository,
      PsychologicalSignalRepository signalRepository) {
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.schoolClassRepository = schoolClassRepository;
    this.subjectRepository = subjectRepository;
    this.assignmentRepository = assignmentRepository;
    this.submissionRepository = submissionRepository;
    this.gradeRepository = gradeRepository;
    this.signalRepository = signalRepository;
  }

  public record PsychAlertCounts(int high, int medium) {}

  public record Dashboard(
      int totalClasses,
      int totalStudents,
      int totalTeachers,
      int totalSubjects,
      int totalAssignments,
      int activeToday,
      double homeworkSubmissionRate,
      List<ClassProgressRow> classProgressList,
      List<TeacherRankingRow> teacherRankings,
      PsychAlertCounts psychologicalAlerts) {}

  public Dashboard dashboard(UUID schoolId) {
    int totalClasses =
        Math.toIntExact(schoolClassRepository.countBySchoolIdAndIsActiveTrue(schoolId));
    int totalStudents = studentProfileRepository.countBySchoolIdAndIsActiveTrue(schoolId);
    int totalTeachers = userRepository.countByRoleAndSchoolId(Role.TEACHER, schoolId);
    int totalSubjects = Math.toIntExact(subjectRepository.countBySchoolId(schoolId));
    int totalAssignments = Math.toIntExact(assignmentRepository.countBySchoolId(schoolId));
    int activeToday =
        submissionRepository.countDistinctStudentsSubmittedSince(
            schoolId, LocalDateTime.now().toLocalDate().atStartOfDay());
    int activeInWindow =
        submissionRepository.countDistinctStudentsSubmittedSince(
            schoolId, LocalDateTime.now().minusDays(SUBMISSION_RATE_WINDOW_DAYS));
    double submissionRate = totalStudents == 0 ? 0.0 : (activeInWindow * 100.0) / totalStudents;

    LocalDateTime scoreWindowSince = LocalDateTime.now().minusDays(DASHBOARD_SCORE_WINDOW_DAYS);
    List<ClassProgressRow> classProgressList =
        gradeRepository.classProgress(schoolId, null, scoreWindowSince);
    List<TeacherRankingRow> teacherRankings = gradeRepository.teacherRanking(schoolId);

    PsychAlertCounts psychAlerts =
        new PsychAlertCounts(
            signalRepository.countUnresolvedBySchoolAndSeverity(
                schoolId, SignalSeverity.HIGH.name()),
            signalRepository.countUnresolvedBySchoolAndSeverity(
                schoolId, SignalSeverity.MEDIUM.name()));

    return new Dashboard(
        totalClasses,
        totalStudents,
        totalTeachers,
        totalSubjects,
        totalAssignments,
        activeToday,
        Math.round(submissionRate * 10) / 10.0,
        classProgressList,
        teacherRankings,
        psychAlerts);
  }
}
