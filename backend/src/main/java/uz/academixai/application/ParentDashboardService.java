package uz.academixai.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.application.StudentDashboardService.RecentGrade;
import uz.academixai.application.StudentSubmissionService.StudentHomeworkItem;
import uz.academixai.domain.ParentStudentLink;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;

/**
 * academix_tz.md §2.5 "Parent API" — {@code GET /parent/dashboard}, {@code GET /parent/children},
 * {@code GET /parent/children/{studentId}/overview}. Response shapes for {@code /children} and
 * {@code /overview} aren't spelled out in the spec (only {@code /dashboard}'s shape is given
 * verbatim) — inferred from the same {@code children} summary shape, reused consistently.
 *
 * <p>Authorization: every method checks {@link ParentLinkService#requireLinkedChild} rather than
 * comparing school IDs — a parent's JWT {@code schoolId} claim collapses to their first linked
 * child (see {@code SchoolContextResolver}), which would incorrectly deny access to a second child
 * at a different school; the link itself is the real authorization boundary here.
 */
@Service
public class ParentDashboardService {

  private final ParentLinkService parentLinkService;
  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository classRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final GradeRepository gradeRepository;
  private final StudentSubmissionService studentSubmissionService;

  public ParentDashboardService(
      ParentLinkService parentLinkService,
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository classRepository,
      HomeworkSubmissionRepository submissionRepository,
      GradeRepository gradeRepository,
      StudentSubmissionService studentSubmissionService) {
    this.parentLinkService = parentLinkService;
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.classRepository = classRepository;
    this.submissionRepository = submissionRepository;
    this.gradeRepository = gradeRepository;
    this.studentSubmissionService = studentSubmissionService;
  }

  public record ChildSummary(
      UUID studentId,
      String name,
      String className,
      boolean todayActivity,
      int pendingHomeworkCount,
      RecentGrade recentGrade,
      boolean biometricConsentGiven) {}

  public record Dashboard(List<ChildSummary> children) {}

  public Dashboard dashboard(UUID parentUserId) {
    return new Dashboard(children(parentUserId));
  }

  public List<ChildSummary> children(UUID parentUserId) {
    return parentLinkService.myChildren(parentUserId).stream()
        .map(link -> buildSummary(link.studentUserId(), link.biometricConsentGiven()))
        .toList();
  }

  public record ChildOverview(
      ChildSummary summary,
      List<StudentHomeworkItem> pendingHomework,
      List<RecentGrade> recentGrades) {}

  private static final int RECENT_GRADES_LIMIT = 5;

  public ChildOverview overview(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    boolean consentGiven =
        parentLinkService.myChildren(parentUserId).stream()
            .filter(link -> link.studentUserId().equals(studentId))
            .findFirst()
            .map(ParentStudentLink::biometricConsentGiven)
            .orElse(false);
    ChildSummary summary = buildSummary(studentId, consentGiven);
    UUID schoolId =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::getSchoolId)
            .orElse(null);
    List<StudentHomeworkItem> pendingHomework =
        schoolId == null
            ? List.of()
            : studentSubmissionService.listHomework(schoolId, studentId).stream()
                .filter(h -> "PENDING".equals(h.submissionStatus()))
                .toList();
    List<RecentGrade> recentGrades =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
            .filter(s -> s.toDomain().status() == SubmissionStatus.GRADED)
            .limit(RECENT_GRADES_LIMIT)
            .map(s -> gradeRepository.findBySubmissionId(s.getId()))
            .flatMap(Optional::stream)
            .map(GradeEntity::toDomain)
            .map(
                g -> new RecentGrade(g.submissionId(), g.score(), g.fivePointGrade(), g.gradedAt()))
            .toList();
    return new ChildOverview(summary, pendingHomework, recentGrades);
  }

  private ChildSummary buildSummary(UUID studentId, boolean biometricConsentGiven) {
    String name =
        userRepository
            .findById(studentId)
            .map(u -> u.getFirstName() + " " + u.getLastName())
            .orElse("");
    String className =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::getClassId)
            .flatMap(classRepository::findById)
            .map(SchoolClassEntity::getFullName)
            .orElse("");
    LocalDate today = LocalDate.now();
    boolean todayActivity =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
            .findFirst()
            .map(s -> s.toDomain().submittedAt().toLocalDate().equals(today))
            .orElse(false);
    int pendingHomeworkCount =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::getSchoolId)
            .map(schoolId -> studentSubmissionService.listHomework(schoolId, studentId))
            .map(
                list ->
                    (int) list.stream().filter(h -> "PENDING".equals(h.submissionStatus())).count())
            .orElse(0);
    RecentGrade recentGrade =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
            .filter(s -> s.toDomain().status() == SubmissionStatus.GRADED)
            .findFirst()
            .flatMap(s -> gradeRepository.findBySubmissionId(s.getId()))
            .map(GradeEntity::toDomain)
            .map(
                g -> new RecentGrade(g.submissionId(), g.score(), g.fivePointGrade(), g.gradedAt()))
            .orElse(null);
    return new ChildSummary(
        studentId,
        name,
        className,
        todayActivity,
        pendingHomeworkCount,
        recentGrade,
        biometricConsentGiven);
  }
}
