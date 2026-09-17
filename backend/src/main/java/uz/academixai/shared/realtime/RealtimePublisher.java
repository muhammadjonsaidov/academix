package uz.academixai.shared.realtime;

import java.util.UUID;

/**
 * Shared-kernel realtime contract: push an event to one user's live SSE stream once the caller's
 * transaction has committed.
 *
 * <p>Exists for the same reason {@link uz.academixai.shared.tenancy.TenantScope} does — the
 * interface lives in the shared kernel and the adapter ({@code
 * uz.academixai.infrastructure.realtime.RealtimeEventBus}) implements it, so a context can publish
 * without importing the legacy infrastructure tree. NotificationService depended on that bus
 * directly until this port existed.
 */
public interface RealtimePublisher {

  /**
   * Publishes after the current transaction commits, or immediately when none is active. Publishing
   * before commit would let a client observe a state the database does not have yet.
   */
  void publishAfterCommit(UUID userId, String eventName, Object payload);
}
