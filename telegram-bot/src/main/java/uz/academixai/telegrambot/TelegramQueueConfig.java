package uz.academixai.telegrambot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code notifications.telegram.queue} — same retry/DLX/TTL topology shape as backend's own {@code
 * HomeworkQueueConfig} (3 retries -> DLX, not requeued forever). Declared identically on both the
 * producer (backend) and consumer (this service) sides — RabbitMQ queue/exchange declaration is
 * idempotent, so whichever process starts first creates the real topology and the other's
 * declaration is a no-op confirmation, not a conflict.
 */
@Configuration
public class TelegramQueueConfig {

  public static final String NOTIFICATIONS_QUEUE = "notifications.telegram.queue";
  private static final String NOTIFICATIONS_DLX = "notifications.telegram.dlx";
  private static final String NOTIFICATIONS_DLQ = "notifications.telegram.queue.dlq";
  private static final long QUEUE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);
  private static final int MAX_RETRY_ATTEMPTS = 3;

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

  // Backend and this service have the message DTO in different packages (each side owns its own
  // copy — no shared module between the two Gradle projects). Jackson2JsonMessageConverter
  // defaults to matching by fully-qualified class name via the __TypeId__ header, which would
  // fail cross-service; a fixed type-id string (matching the one backend's own producer-side
  // converter sets, see backend's TelegramNotificationQueueConfig) sidesteps that entirely.
  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
    DefaultClassMapper classMapper = new DefaultClassMapper();
    classMapper.setIdClassMapping(
        java.util.Map.of("telegramNotification", TelegramNotificationMessage.class));
    converter.setClassMapper(classMapper);
    return converter;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(retryInterceptor());
    return factory;
  }

  private MethodInterceptor retryInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .maxRetries(MAX_RETRY_ATTEMPTS)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build();
  }
}
