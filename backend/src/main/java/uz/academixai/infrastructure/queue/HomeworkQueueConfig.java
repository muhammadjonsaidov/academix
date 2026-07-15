package uz.academixai.infrastructure.queue;

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
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * academix_backend_tdd.md §6.3 — {@code homework.submissions.queue}: 3-attempt retry, then
 * dead-lettered; 10-minute TTL. Retry is Spring Retry (via the listener container's advice chain),
 * not manual redelivery-count tracking — after 3 failed attempts the recoverer rejects the message
 * without requeueing, which RabbitMQ then routes to the DLX per the queue's {@code
 * x-dead-letter-exchange} argument. TTL is enforced the same way: an unconsumed message expires
 * after 10 minutes and is dead-lettered by RabbitMQ itself, not application code.
 */
@Configuration
public class HomeworkQueueConfig {

  public static final String SUBMISSIONS_QUEUE = "homework.submissions.queue";
  private static final String SUBMISSIONS_DLX = "homework.submissions.dlx";
  private static final String SUBMISSIONS_DLQ = "homework.submissions.queue.dlq";
  private static final long QUEUE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);
  private static final int MAX_RETRY_ATTEMPTS = 3;

  @Bean
  public DirectExchange homeworkSubmissionsDlx() {
    return new DirectExchange(SUBMISSIONS_DLX);
  }

  @Bean
  public Queue homeworkSubmissionsDlq() {
    return QueueBuilder.durable(SUBMISSIONS_DLQ).build();
  }

  @Bean
  public Binding homeworkSubmissionsDlqBinding() {
    // Same routing key as the main queue's name — a dead-lettered message keeps its original
    // routing key, so binding the DLQ with that key is what actually catches it.
    return BindingBuilder.bind(homeworkSubmissionsDlq())
        .to(homeworkSubmissionsDlx())
        .with(SUBMISSIONS_QUEUE);
  }

  @Bean
  public Queue homeworkSubmissionsQueue() {
    return QueueBuilder.durable(SUBMISSIONS_QUEUE)
        .deadLetterExchange(SUBMISSIONS_DLX)
        .deadLetterRoutingKey(SUBMISSIONS_QUEUE)
        .ttl((int) QUEUE_TTL_MILLIS)
        .build();
  }

  @Bean
  public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter);
    // Exhausted retries must NOT requeue — requeueing would just retry forever instead of
    // dead-lettering, defeating the whole point of the DLX.
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(retryInterceptor());
    return factory;
  }

  // spring-amqp's own RetryInterceptorBuilder (org.springframework.amqp.rabbit.config), not the
  // generic org.springframework.retry.interceptor one — RejectAndDontRequeueRecoverer implements
  // AMQP's MessageRecoverer, not spring-retry's MethodInvocationRecoverer; mixing the two builder
  // families doesn't compile. This builder's own API changed too in the version Boot 4.1 pulls in:
  // maxRetries(int), not maxAttempts(int), and build() now returns org.aopalliance.intercept.
  // MethodInterceptor (a bridge method masks the real StatelessRetryOperationsInterceptor type),
  // not spring-retry's RetryOperationsInterceptor.
  private MethodInterceptor retryInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .maxRetries(MAX_RETRY_ATTEMPTS)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build();
  }
}
