package uz.academixai.progress.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.progress.domain.XpHistoryEntry;

/**
 * Progress' published read API for a student's XP ledger.
 *
 * <p>Wellbeing's nightly behavior analysis reads recent XP as an activity signal; before this
 * existed it reached into Progress' JPA repository directly, which is exactly the cross-context
 * coupling the modular monolith is migrating away from.
 */
public interface StudentXpHistory {

  /** Ledger entries for one student, newest first. */
  List<XpHistoryEntry> recentOf(UUID studentId);
}
