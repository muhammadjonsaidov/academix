package uz.academixai.interfaces.web.psychologist;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.WatchlistEntryWithStudent;

public record WatchlistStudentResponse(
    UUID studentId, String studentName, String reason, LocalDateTime addedAt) {

  public static WatchlistStudentResponse from(WatchlistEntryWithStudent item) {
    return new WatchlistStudentResponse(
        item.entry().studentId(),
        item.studentName(),
        item.entry().reason(),
        item.entry().addedAt());
  }
}
