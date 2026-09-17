package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import uz.academixai.progress.application.port.in.StudentDashboard.XpHistoryItem;

public record XpHistoryResponse(LocalDateTime date, int xp, String reason) {

  public static XpHistoryResponse from(XpHistoryItem item) {
    return new XpHistoryResponse(item.date(), item.xp(), item.reason());
  }
}
