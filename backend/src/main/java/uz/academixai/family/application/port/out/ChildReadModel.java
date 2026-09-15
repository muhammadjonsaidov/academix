package uz.academixai.family.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.StudentProfile;
import uz.academixai.progress.application.port.in.StudentDashboard.RecentGrade;

/**
 * The child facts a parent view needs, which Family does not own: the student's profile (Learning/
 * School), their display name (Identity), their class (School) and their graded work (Learning /
 * Progress).
 *
 * <p>Deliberately ONE coarse read port rather than four narrow ones: this is the query side of a
 * dashboard, and the roadmap's CQRS-lite rule keeps dashboard projections out of the write
 * aggregates. The adapter behind it is a named compatibility adapter — those reads move to each
 * owning context's published read API as School/Learning finish migrating.
 */
public interface ChildReadModel {

  Optional<StudentProfile> profile(UUID studentUserId);

  /** Display name plus the academic facts a child summary shows. */
  record ChildFacts(
      String studentName,
      UUID classId,
      String className,
      boolean activeToday,
      RecentGrade lastGrade) {}

  /**
   * Never empty: a student without a profile yet still has a name and submissions, and the parent
   * summary used to render that case (name empty, no class) rather than failing.
   */
  ChildFacts facts(UUID studentUserId);

  /** Graded submissions, newest first, capped by the caller's view size. */
  List<RecentGrade> recentGrades(UUID studentUserId, int limit);
}
