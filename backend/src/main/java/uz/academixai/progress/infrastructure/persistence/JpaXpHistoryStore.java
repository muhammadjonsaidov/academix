package uz.academixai.progress.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import uz.academixai.progress.application.port.out.XpHistoryStore;
import uz.academixai.progress.domain.XpHistoryEntry;

/** JPA adapter for the immutable XP ledger. */
@Repository
public class JpaXpHistoryStore implements XpHistoryStore {

  private final XpHistoryRepository history;

  public JpaXpHistoryStore(XpHistoryRepository history) {
    this.history = history;
  }

  @Override
  public XpHistoryEntry save(XpHistoryEntry entry) {
    return history.save(XpHistoryEntity.fromDomain(entry)).toDomain();
  }
}
