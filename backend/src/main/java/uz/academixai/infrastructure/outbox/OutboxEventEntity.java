package uz.academixai.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Durable event record. Delivery is at-least-once, so consumers must be idempotent. */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

  @Id private UUID id;

  @Column(name = "school_id")
  private UUID schoolId;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(nullable = false)
  private String destination;

  @Column(name = "payload_type", nullable = false)
  private String payloadType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "available_at", nullable = false)
  private LocalDateTime availableAt;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error")
  private String lastError;

  protected OutboxEventEntity() {}

  OutboxEventEntity(
      UUID id,
      UUID schoolId,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String destination,
      String payloadType,
      String payload,
      LocalDateTime occurredAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.destination = destination;
    this.payloadType = payloadType;
    this.payload = payload;
    this.occurredAt = occurredAt;
    this.availableAt = occurredAt;
  }

  public String getDestination() {
    return destination;
  }

  public String getPayloadType() {
    return payloadType;
  }

  public String getPayload() {
    return payload;
  }

  public int getAttempts() {
    return attempts;
  }

  void markPublished(LocalDateTime publishedAt) {
    this.publishedAt = publishedAt;
    this.lastError = null;
  }

  void scheduleRetry(LocalDateTime availableAt, String error) {
    attempts++;
    this.availableAt = availableAt;
    lastError =
        error == null
            ? "Unknown delivery error"
            : error.substring(0, Math.min(2000, error.length()));
  }
}
