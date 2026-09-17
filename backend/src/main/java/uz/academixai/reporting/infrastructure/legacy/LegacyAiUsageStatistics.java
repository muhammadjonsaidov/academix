package uz.academixai.reporting.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository;
import uz.academixai.reporting.application.port.out.AiUsageStatistics;

/** Adapter for {@link AiUsageStatistics} over the legacy {@code AiUsageLogRepository}. */
@Component
public class LegacyAiUsageStatistics implements AiUsageStatistics {

  private final AiUsageLogRepository usage;

  public LegacyAiUsageStatistics(AiUsageLogRepository usage) {
    this.usage = usage;
  }

  @Override
  public List<ByClass> byClass(UUID schoolId, LocalDateTime since) {
    return usage.usageByClass(schoolId, since).stream()
        .map(row -> new ByClass(row.getClassId(), row.getClassName(), row.getCallCount()))
        .toList();
  }

  @Override
  public List<BySubject> bySubject(UUID schoolId, LocalDateTime since) {
    return usage.usageBySubject(schoolId, since).stream()
        .map(row -> new BySubject(row.getSubjectId(), row.getSubjectName(), row.getCallCount()))
        .toList();
  }

  @Override
  public List<ByTeacher> byTeacher(UUID schoolId, LocalDateTime since) {
    return usage.usageByTeacher(schoolId, since).stream()
        .map(
            row ->
                new ByTeacher(
                    row.getTeacherId(), row.getFirstName(), row.getLastName(), row.getCallCount()))
        .toList();
  }
}
