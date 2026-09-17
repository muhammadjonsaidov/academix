package uz.academixai.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes integration events in the same transaction as the originating business operation. */
@Service
public class OutboxService {

  private final OutboxEventRepository events;
  private final ObjectMapper objectMapper;

  public OutboxService(OutboxEventRepository events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueue(
      UUID schoolId,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String destination,
      String payloadType,
      Object payload) {
    try {
      events.save(
          new OutboxEventEntity(
              UUID.randomUUID(),
              schoolId,
              aggregateType,
              aggregateId,
              eventType,
              destination,
              payloadType,
              objectMapper.writeValueAsString(payload),
              LocalDateTime.now()));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize outbox event " + eventType, exception);
    }
  }
}
