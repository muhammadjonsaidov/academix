package uz.academixai.reporting.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.reporting.application.port.out.LearningActivityCounts;
import uz.academixai.reporting.application.port.out.SchoolDirectoryCounts;
import uz.academixai.reporting.application.port.out.WellbeingAlertCounts;

/**
 * Adapters for the three admin-dashboard count ports, grouped in one class because they share the
 * same shape (one repository count per method) and splitting them would repeat the same five lines
 * of constructor ceremony three times.
 *
 * <p>Each port is still the seam that matters: the dashboard use case names the fact it needs, and
 * this is the only place that knows which table currently serves it.
 */
@Component
public class LegacyDashboardCounts
    implements SchoolDirectoryCounts, LearningActivityCounts, WellbeingAlertCounts {

  private final UserRepository users;
  private final StudentProfileRepository students;
  private final SchoolClassRepository classes;
  private final SubjectRepository subjects;
  private final ClassSubjectTeacherRepository assignments;
  private final HomeworkSubmissionRepository submissions;
  private final PsychologicalSignalRepository signals;

  public LegacyDashboardCounts(
      UserRepository users,
      StudentProfileRepository students,
      SchoolClassRepository classes,
      SubjectRepository subjects,
      ClassSubjectTeacherRepository assignments,
      HomeworkSubmissionRepository submissions,
      PsychologicalSignalRepository signals) {
    this.users = users;
    this.students = students;
    this.classes = classes;
    this.subjects = subjects;
    this.assignments = assignments;
    this.submissions = submissions;
    this.signals = signals;
  }

  @Override
  public Counts of(UUID schoolId) {
    return new Counts(
        Math.toIntExact(classes.countBySchoolIdAndIsActiveTrue(schoolId)),
        students.countBySchoolIdAndIsActiveTrue(schoolId),
        users.countByRoleAndSchoolId(Role.TEACHER, schoolId),
        Math.toIntExact(subjects.countBySchoolId(schoolId)));
  }

  @Override
  public long assignments(UUID schoolId) {
    return assignments.countBySchoolId(schoolId);
  }

  @Override
  public int distinctSubmittersSince(UUID schoolId, LocalDateTime since) {
    return submissions.countDistinctStudentsSubmittedSince(schoolId, since);
  }

  @Override
  public int unresolved(UUID schoolId, SignalSeverity severity) {
    return signals.countUnresolvedBySchoolAndSeverity(schoolId, severity.name());
  }
}
