package uz.academixai.infrastructure.queue;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code notifications.telegram.queue} topology — consumed by the standalone {@code telegram-bot/}
 * service, not by this backend. Declared here too (not just there) because RabbitMQ queue/exchange
 * declaration is idempotent — whichever process starts first creates the real topology, so backend
 * can publish successfully even if the bot service isn't up yet, rather than failing because the
 * queue doesn't exist.
 *
 * <p>Deliberately does NOT redeclare {@code jsonMessageConverter}/{@code
 * rabbitListenerContainerFactory} — those already exist as shared beans in {@link
 * HomeworkQueueConfig} (reused by {@link ExamQueueConfig} the same way) and redeclaring them here
 * would collide. {@link NotificationTelegramQueueProducer} explicitly overrides the {@code
 * __TypeId__} header per-message instead of touching that shared converter's default (FQCN-based)
 * type mapping, which the two purely-intra-backend queues still rely on unchanged.
 */
@Configuration
public class NotificationTelegramQueueConfig {

  public static final String NOTIFICATIONS_QUEUE = "notifications.telegram.queue";
  private static final String NOTIFICATIONS_DLX = "notifications.telegram.dlx";
  private static final String NOTIFICATIONS_DLQ = "notifications.telegram.queue.dlq";
  private static final long QUEUE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

  @Bean
  public DirectExchange notificationsTelegramDlx() {
    return new DirectExchange(NOTIFICATIONS_DLX);
  }

  @Bean
  public Queue notificationsTelegramDlq() {
    return QueueBuilder.durable(NOTIFICATIONS_DLQ).build();
  }

  @Bean
  public Binding notificationsTelegramDlqBinding() {
    return BindingBuilder.bind(notificationsTelegramDlq())
        .to(notificationsTelegramDlx())
        .with(NOTIFICATIONS_QUEUE);
  }

  @Bean
  public Queue notificationsTelegramQueue() {
    return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
        .deadLetterExchange(NOTIFICATIONS_DLX)
        .deadLetterRoutingKey(NOTIFICATIONS_QUEUE)
        .ttl((int) QUEUE_TTL_MILLIS)
        .build();
  }
}
