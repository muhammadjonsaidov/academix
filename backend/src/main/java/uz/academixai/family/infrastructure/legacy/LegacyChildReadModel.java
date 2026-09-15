package uz.academixai.family.infrastructure.legacy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.StudentProfile;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.family.application.port.out.ChildReadModel;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.progress.application.port.in.StudentDashboard.RecentGrade;

/**
 * Compatibility adapter for the child facts a parent view needs but Family does not own — the
 * student profile (School/Learning), the display name (Identity), the class (School) and graded
 * work (Learning/Progress).
 *
 * <p>Named "Legacy" on purpose, like Progress' other transitional adapters: it reads the legacy
 * persistence package directly because those tables' owning contexts have not published read APIs
 * yet. Each read here disappears as School/Learning finish migrating.
 */
@Component
public class LegacyChildReadModel implements ChildReadModel {

  private final StudentProfileRepository students;
  private final UserRepository users;
  private final SchoolClassRepository classes;
  private final HomeworkSubmissionRepository submissions;
  private final GradeRepository grades;

  public LegacyChildReadModel(
      StudentProfileRepository students,
      UserRepository users,
      SchoolClassRepository classes,
      HomeworkSubmissionRepository submissions,
      GradeRepository grades) {
    this.students = students;
    this.users = users;
    this.classes = classes;
    this.submissions = submissions;
    this.grades = grades;
  }

  @Override
  public Optional<StudentProfile> profile(UUID studentUserId) {
    return students.findByUserId(studentUserId).map(StudentProfileEntity::toDomain);
  }

  @Override
  public ChildFacts facts(UUID studentUserId) {
    Optional<StudentProfile> profile = profile(studentUserId);
    String name =
        users
            .findById(studentUserId)
            .map(user -> user.getFirstName() + " " + user.getLastName())
            .orElse("");
    UUID classId = profile.map(StudentProfile::classId).orElse(null);
    String className =
        Optional.ofNullable(classId)
            .flatMap(classes::findById)
            .map(SchoolClassEntity::getFullName)
            .orElse("");
    List<RecentGrade> graded = recentGrades(studentUserId, 1);
    boolean activeToday =
        submissions.findByStudentIdOrderBySubmittedAtDesc(studentUserId).stream()
            .findFirst()
            .map(
                submission ->
                    submission.toDomain().submittedAt().toLocalDate().equals(LocalDate.now()))
            .orElse(false);
    return new ChildFacts(
        name, classId, className, activeToday, graded.isEmpty() ? null : graded.get(0));
  }

  @Override
  public List<RecentGrade> recentGrades(UUID studentUserId, int limit) {
    return submissions.findByStudentIdOrderBySubmittedAtDesc(studentUserId).stream()
        .filter(submission -> submission.toDomain().status() == SubmissionStatus.GRADED)
        .limit(limit)
        .map(submission -> grades.findBySubmissionId(submission.getId()))
        .flatMap(Optional::stream)
        .map(GradeEntity::toDomain)
        .map(
            grade ->
                new RecentGrade(
                    grade.submissionId(), grade.score(), grade.fivePointGrade(), grade.gradedAt()))
        .toList();
  }
}
