package uz.academixai.progress.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.progress.application.port.in.StudentXpHistory;
import uz.academixai.progress.domain.XpHistoryEntry;

/**
 * Read-model adapter behind Progress' published {@link StudentXpHistory} API — the query side of
 * the XP ledger, which the roadmap keeps separate from the write aggregate.
 */
@Repository
public class JpaStudentXpHistory implements StudentXpHistory {

  private final XpHistoryRepository history;

  public JpaStudentXpHistory(XpHistoryRepository history) {
    this.history = history;
  }

  @Override
  public List<XpHistoryEntry> recentOf(UUID studentId) {
    return history.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
        .map(XpHistoryEntity::toDomain)
        .toList();
  }
}
