package uz.academixai.family.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.application.port.out.ChildReadModel;
import uz.academixai.family.domain.ParentStudentLink;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.application.port.in.StudentDashboard.RecentGrade;

/**
 * academix_tz.md §2.5 "Parent API" — {@code GET /parent/dashboard}, {@code GET /parent/children},
 * {@code GET /parent/children/{studentId}/overview}. Response shapes for {@code /children} and
 * {@code /overview} aren't spelled out in the spec (only {@code /dashboard}'s shape is given
 * verbatim) — inferred from the same {@code children} summary shape, reused consistently.
 *
 * <p>Authorization: every method asks Family's published {@link ParentChildAccess} rather than
 * comparing school IDs — a parent's JWT {@code schoolId} claim collapses to their first linked
 * child (see {@code SchoolMembership}), which would incorrectly deny access to a second child at a
 * different school; the link itself is the real authorization boundary here.
 *
 * <p>Reads come from {@link ChildReadModel} (the query side) and Learning's published homework
 * query; this use case holds no persistence of its own, so it never sees another context's tables.
 */
@Service
public class ParentDashboardService {

  private static final int RECENT_GRADES_LIMIT = 5;
  private static final String PENDING = "PENDING";

  private final ParentChildAccess parentLinks;
  private final ChildReadModel children;
  private final StudentHomeworkQuery studentHomeworkQuery;

  public ParentDashboardService(
      ParentChildAccess parentLinks,
      ChildReadModel children,
      StudentHomeworkQuery studentHomeworkQuery) {
    this.parentLinks = parentLinks;
    this.children = children;
    this.studentHomeworkQuery = studentHomeworkQuery;
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
    return parentLinks.childrenOf(parentUserId).stream()
        .map(link -> buildSummary(link.studentUserId(), link.biometricConsentGiven()))
        .toList();
  }

  public record ChildOverview(
      ChildSummary summary, List<HomeworkItem> pendingHomework, List<RecentGrade> recentGrades) {}

  public ChildOverview overview(UUID parentUserId, UUID studentId) {
    parentLinks.requireLinkedChild(parentUserId, studentId);
    boolean consentGiven =
        parentLinks.childrenOf(parentUserId).stream()
            .filter(link -> link.studentUserId().equals(studentId))
            .findFirst()
            .map(ParentStudentLink::biometricConsentGiven)
            .orElse(false);
    ChildSummary summary = buildSummary(studentId, consentGiven);

    // A student with no class assignment yet (classId null) legitimately has no homework —
    // listHomework's requireStudentClassId would 403 the parent's whole children list otherwise,
    // confirmed by a real request against seeded class-less students.
    UUID classId = children.facts(studentId).classId();
    List<HomeworkItem> pendingHomework =
        Optional.ofNullable(classId)
            .flatMap(
                ignored ->
                    children
                        .profile(studentId)
                        .map(
                            profile ->
                                studentHomeworkQuery.listHomework(profile.schoolId(), studentId)))
            .orElse(List.of())
            .stream()
            .filter(homework -> PENDING.equals(homework.submissionStatus()))
            .toList();

    return new ChildOverview(
        summary, pendingHomework, children.recentGrades(studentId, RECENT_GRADES_LIMIT));
  }

  private ChildSummary buildSummary(UUID studentId, boolean biometricConsentGiven) {
    ChildReadModel.ChildFacts facts = children.facts(studentId);
    int pendingHomeworkCount =
        Optional.ofNullable(facts.classId())
            .flatMap(
                ignored ->
                    children
                        .profile(studentId)
                        .map(
                            profile ->
                                studentHomeworkQuery.listHomework(profile.schoolId(), studentId)))
            .map(
                list ->
                    (int) list.stream().filter(h -> PENDING.equals(h.submissionStatus())).count())
            .orElse(0);
    return new ChildSummary(
        studentId,
        facts.studentName(),
        facts.className(),
        facts.activeToday(),
        pendingHomeworkCount,
        facts.lastGrade(),
        biometricConsentGiven);
  }
}
