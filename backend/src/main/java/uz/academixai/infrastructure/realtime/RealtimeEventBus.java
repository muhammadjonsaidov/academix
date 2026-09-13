package uz.academixai.infrastructure.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory SSE hub — the project's real-time push channel (the one place the frontend gets events
 * without polling; see {@code useRealtimeEvents} on the frontend and the docs' old "no push
 * updates" stance, which this deliberately supersedes for status/notification events).
 *
 * <p>One {@link SseEmitter} per browser tab, registered per authenticated userId. Publishing is a
 * fan-out to that user's emitters. No persistence, no replay, no multi-instance fan-out: with a
 * single backend instance (the current deploy shape) this is complete; a horizontal deployment
 * would need the events routed through RabbitMQ (the {@code telegram-bot} service already owns a
 * fanout pattern to copy).
 *
 * <p><b>Transaction safety:</b> {@link #publishAfterCommit} is the entry point used by services
 * that run inside a transaction (the RabbitMQ listeners, notification sends) — the event only fires
 * after the DB commit, so a client can never observe a status that the DB doesn't yet have (a
 * rollback would silently drop the event instead of lying to the client).
 */
@Component
public class RealtimeEventBus {

  private static final Logger log = LoggerFactory.getLogger(RealtimeEventBus.class);

  // 0 = never time out server-side; the emitter only ends via completion/error/close.
  private static final long EMITTER_TIMEOUT_MS = 0L;

  private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

  /**
   * Register an emitter for a user. The returned emitter is owned by the caller (the HTTP
   * controller) and pushed to by this bus until it completes, times out, or errors.
   */
  public SseEmitter subscribe(UUID userId) {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    subscribers.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(userId, emitter));
    emitter.onTimeout(() -> remove(userId, emitter));
    emitter.onError(ignored -> remove(userId, emitter));
    return emitter;
  }

  /**
   * Publish after the current transaction commits if one is active, otherwise immediately. See the
   * class Javadoc for why this matters for the AI/notification pipelines.
   */
  public void publishAfterCommit(UUID userId, String eventName, Object payload) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              publish(userId, eventName, payload);
            }
          });
    } else {
      publish(userId, eventName, payload);
    }
  }

  /** Fan the event out to all of the user's live connections. No-op when none are connected. */
  public void publish(UUID userId, String eventName, Object payload) {
    List<SseEmitter> emitters = subscribers.get(userId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload));
      } catch (IOException | IllegalStateException e) {
        emitter.completeWithError(e);
        remove(userId, emitter);
      }
    }
  }

  private void remove(UUID userId, SseEmitter emitter) {
    List<SseEmitter> emitters = subscribers.get(userId);
    if (emitters != null) {
      emitters.remove(emitter);
      if (emitters.isEmpty()) {
        subscribers.remove(userId, emitters);
      }
    }
  }

  /**
   * Keeps idle connections alive through proxies that close silent streams (Railway's edge, nginx
   * with short proxy_read_timeout, ...). Comment lines are valid SSE and invisible to EventSource's
   * message listeners, so this adds no client-side noise.
   */
  @Scheduled(fixedDelay = 20_000)
  public void heartbeat() {
    int sent = 0;
    for (List<SseEmitter> emitters : subscribers.values()) {
      for (SseEmitter emitter : emitters) {
        try {
          emitter.send(SseEmitter.event().comment("hb"));
          sent++;
        } catch (IOException | IllegalStateException e) {
          emitter.completeWithError(e);
        }
      }
    }
    if (sent > 0) {
      log.debug("SSE heartbeat sent to {} connections", sent);
    }
  }
}
