package uz.academixai.wellbeing.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import uz.academixai.progress.domain.XpHistoryEntry;

/** Read boundary for privacy-minimized activity metadata used by behavior analysis. */
public interface BehaviorActivityLookup {

  record Activity(
      List<LocalDateTime> submissionTimes,
      long recentXp,
      List<XpHistoryEntry> xpHistory,
      LocalDate lastSubmissionDate,
      List<String> recentChatMessages) {}

  Activity find(UUID schoolId, UUID studentId, LocalDateTime since, int maxChatMessages);
}
