package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import uz.academixai.application.AdminAnalyticsService.AiUsage;

/** academix_tz.md §8 {@code GET /admin/analytics/ai-usage} — exact response shape. */
public record AiUsageResponse(
    List<ByClass> byClass, List<BySubject> bySubject, List<ByTeacher> byTeacher) {

  public record ByClass(UUID classId, String className, long callCount) {}

  public record BySubject(UUID subjectId, String subjectName, long callCount) {}

  public record ByTeacher(UUID teacherId, String firstName, String lastName, long callCount) {}

  public static AiUsageResponse from(AiUsage usage) {
    return new AiUsageResponse(
        usage.byClass().stream()
            .map(r -> new ByClass(r.getClassId(), r.getClassName(), r.getCallCount()))
            .toList(),
        usage.bySubject().stream()
            .map(r -> new BySubject(r.getSubjectId(), r.getSubjectName(), r.getCallCount()))
            .toList(),
        usage.byTeacher().stream()
            .map(
                r ->
                    new ByTeacher(
                        r.getTeacherId(), r.getFirstName(), r.getLastName(), r.getCallCount()))
            .toList());
  }
}
