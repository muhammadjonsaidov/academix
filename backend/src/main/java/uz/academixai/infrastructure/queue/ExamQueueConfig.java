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
 * academix_backend_tdd.md §6.7 — {@code exam.submissions.queue}: same 3-attempt retry → DLX shape
 * as {@link HomeworkQueueConfig}, longer 15-minute TTL (bulk exam uploads are higher volume than
 * one-at-a-time homework submissions). Reuses the shared {@code rabbitListenerContainerFactory}
 * bean from {@link HomeworkQueueConfig} — the retry policy is queue-agnostic, only the TTL (set on
 * the {@link Queue} bean itself, not the factory) differs per queue.
 */
@Configuration
public class ExamQueueConfig {

  public static final String SUBMISSIONS_QUEUE = "exam.submissions.queue";
  private static final String SUBMISSIONS_DLX = "exam.submissions.dlx";
  private static final String SUBMISSIONS_DLQ = "exam.submissions.queue.dlq";
  private static final long QUEUE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);

  @Bean
  public DirectExchange examSubmissionsDlx() {
    return new DirectExchange(SUBMISSIONS_DLX);
  }

  @Bean
  public Queue examSubmissionsDlq() {
    return QueueBuilder.durable(SUBMISSIONS_DLQ).build();
  }

  @Bean
  public Binding examSubmissionsDlqBinding() {
    return BindingBuilder.bind(examSubmissionsDlq())
        .to(examSubmissionsDlx())
        .with(SUBMISSIONS_QUEUE);
  }

  @Bean
  public Queue examSubmissionsQueue() {
    return QueueBuilder.durable(SUBMISSIONS_QUEUE)
        .deadLetterExchange(SUBMISSIONS_DLX)
        .deadLetterRoutingKey(SUBMISSIONS_QUEUE)
        .ttl((int) QUEUE_TTL_MILLIS)
        .build();
  }
}
