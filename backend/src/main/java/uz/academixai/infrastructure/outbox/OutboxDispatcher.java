package uz.academixai.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.infrastructure.queue.SubmissionQueueMessage;
import uz.academixai.infrastructure.queue.SyllabusIngestionMessage;
import uz.academixai.infrastructure.queue.TelegramNotificationMessage;

/** Polls committed outbox records. A failed publish remains durable and is retried with backoff. */
@Component
public class OutboxDispatcher {

  private static final int BATCH_SIZE = 50;
  private static final String SUBMISSION_PAYLOAD = "submission";
  private static final String TELEGRAM_PAYLOAD = "telegramNotification";
  private static final String SYLLABUS_INGESTION_PAYLOAD = "syllabusIngestion";

  private final OutboxEventRepository events;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public OutboxDispatcher(
      OutboxEventRepository events, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.events = events;
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${academix.outbox.poll-interval-ms:1000}")
  @Transactional
  public void dispatchPending() {
    LocalDateTime now = LocalDateTime.now();
    List<OutboxEventEntity> pending = events.lockPending(now, PageRequest.of(0, BATCH_SIZE));
    for (OutboxEventEntity event : pending) {
      try {
        publish(event);
        event.markPublished(now);
      } catch (Exception exception) {
        event.scheduleRetry(now.plusSeconds(retryDelaySeconds(event)), exception.getMessage());
      }
    }
  }

  private void publish(OutboxEventEntity event) throws JsonProcessingException {
    switch (event.getPayloadType()) {
      case SUBMISSION_PAYLOAD ->
          rabbitTemplate.convertAndSend(
              event.getDestination(),
              objectMapper.readValue(event.getPayload(), SubmissionQueueMessage.class));
      case TELEGRAM_PAYLOAD -> publishTelegram(event);
      case SYLLABUS_INGESTION_PAYLOAD ->
          rabbitTemplate.convertAndSend(
              event.getDestination(),
              objectMapper.readValue(event.getPayload(), SyllabusIngestionMessage.class));
      default ->
          throw new IllegalArgumentException(
              "Unsupported outbox payload type: " + event.getPayloadType());
    }
  }

  private void publishTelegram(OutboxEventEntity event) throws JsonProcessingException {
    TelegramNotificationMessage payload =
        objectMapper.readValue(event.getPayload(), TelegramNotificationMessage.class);
    MessagePostProcessor typeIdOverride =
        (Message message) -> {
          message.getMessageProperties().setHeader("__TypeId__", TELEGRAM_PAYLOAD);
          return message;
        };
    rabbitTemplate.convertAndSend(event.getDestination(), payload, typeIdOverride);
  }

  private static long retryDelaySeconds(OutboxEventEntity event) {
    return Math.min(300, 1L << Math.min(8, event.getAttempts()));
  }
}
