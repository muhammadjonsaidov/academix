package uz.academixai.progress.application.port.out;

import uz.academixai.progress.domain.XpHistoryEntry;

/** Immutable XP ledger write boundary. */
public interface XpHistoryStore {

  XpHistoryEntry save(XpHistoryEntry entry);
}
