package uz.academixai.intelligence.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the weekly handwriting-reset audit.
 *
 * <p>Two reads and no writes, because the audit only flags: backend_tdd.md §6.5 deliberately does
 * not auto-block a student or a teacher, so nothing here can change a profile.
 *
 * <p>One id per reset log rather than a pre-grouped count: grouping is the audit's own statistics,
 * and doing it in the adapter would hide the mean and variance it computes from the only place that
 * can explain them.
 */
public interface HandwritingResetAudit {

  /** Students whose handwriting profile has been reset at least {@code minResets} times. */
  List<UUID> studentIdsWithAtLeastResets(int minResets);

  /** Every reset log's initiating teacher — repeated once per reset, so the caller can count. */
  List<UUID> initiatingTeacherIds();
}
