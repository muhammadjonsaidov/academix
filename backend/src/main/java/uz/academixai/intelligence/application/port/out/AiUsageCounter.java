package uz.academixai.intelligence.application.port.out;

import java.util.UUID;
import uz.academixai.intelligence.domain.AiCallCategory;

/** Fast, month-scoped counter boundary for consumed AI calls. */
public interface AiUsageCounter {

  long currentUsage(UUID schoolId, AiCallCategory category);

  void increment(UUID schoolId, AiCallCategory category);
}
