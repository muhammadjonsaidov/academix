package uz.academixai.reporting.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the AI cost showback breakdown (academix_tz.md §8) — grading calls only
 * (HOMEWORK/EXAM), never CHAT, mirroring how {@code ai_usage_log} is written.
 */
public interface AiUsageStatistics {

  record ByClass(UUID classId, String className, long callCount) {}

  record BySubject(UUID subjectId, String subjectName, long callCount) {}

  record ByTeacher(UUID teacherId, String firstName, String lastName, long callCount) {}

  List<ByClass> byClass(UUID schoolId, LocalDateTime since);

  List<BySubject> bySubject(UUID schoolId, LocalDateTime since);

  List<ByTeacher> byTeacher(UUID schoolId, LocalDateTime since);
}
