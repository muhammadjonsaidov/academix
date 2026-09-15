package uz.academixai.infrastructure.queue;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Durable knowledge-indexing queue with the same retry/DLX safety model as submission analysis. */
@Configuration
public class SyllabusIngestionQueueConfig {

  public static final String INGESTION_QUEUE = "syllabus.ingestion.queue";
  private static final String INGESTION_DLX = "syllabus.ingestion.dlx";
  private static final String INGESTION_DLQ = "syllabus.ingestion.queue.dlq";

  @Bean
  public DirectExchange syllabusIngestionDlx() {
    return new DirectExchange(INGESTION_DLX);
  }

  @Bean
  public Queue syllabusIngestionDlq() {
    return QueueBuilder.durable(INGESTION_DLQ).build();
  }

  @Bean
  public Binding syllabusIngestionDlqBinding() {
    return BindingBuilder.bind(syllabusIngestionDlq())
        .to(syllabusIngestionDlx())
        .with(INGESTION_QUEUE);
  }

  @Bean
  public Queue syllabusIngestionQueue() {
    return QueueBuilder.durable(INGESTION_QUEUE)
        .deadLetterExchange(INGESTION_DLX)
        .deadLetterRoutingKey(INGESTION_QUEUE)
        .ttl((int) TimeUnit.MINUTES.toMillis(30))
        .build();
  }
}
