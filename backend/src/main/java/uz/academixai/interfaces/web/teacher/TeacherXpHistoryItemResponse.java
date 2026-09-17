package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import uz.academixai.progress.domain.XpHistoryEntry;

public record TeacherXpHistoryItemResponse(LocalDateTime date, int xp, String reason) {

  public static TeacherXpHistoryItemResponse from(XpHistoryEntry entry) {
    return new TeacherXpHistoryItemResponse(entry.occurredAt(), entry.xp(), entry.reason());
  }
}
