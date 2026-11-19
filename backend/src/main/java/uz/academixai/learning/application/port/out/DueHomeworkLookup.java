package uz.academixai.learning.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Students who still owe an assignment whose deadline falls inside a time window. */
public interface DueHomeworkLookup {

  record DueHomework(UUID studentId, UUID assignmentId, String title, LocalDateTime deadlineAt) {}

  /** Across every school — the reminder sweep is cross-tenant by definition. */
  List<DueHomework> unsubmittedWithDeadlineBetween(LocalDateTime from, LocalDateTime to);
}
