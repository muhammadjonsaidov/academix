package uz.academixai.interfaces.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uz.academixai.TestcontainersConfiguration;

/**
 * The liveness/readiness probes an orchestrator calls, and the fact that it can call them at all.
 *
 * <p>Spring Security matches its requestMatchers exactly, so permitting {@code /actuator/health}
 * alone left {@code /actuator/health/liveness} and {@code /actuator/health/readiness} behind the
 * authentication filter: the probes returned 401 no matter how healthy the JVM was, and a
 * Kubernetes/Docker healthcheck reading that as "not alive" would have restarted a working
 * instance. That is the regression this file pins down — it is about reachability first and the
 * reported status second.
 *
 * <p>No token is sent on purpose. Liveness is expected to answer UP even while a dependency is
 * down, which is exactly why it is asserted separately from readiness.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HealthProbeIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void livenessAnswersWithoutAToken() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void readinessAnswersWithoutAToken() throws Exception {
    // Testcontainers runs Postgres, Redis and RabbitMQ for this context, so readiness is genuinely
    // UP here — the assertion is that it is reachable and reports, not merely that it exists.
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void theAggregateHealthEndpointStillAnswers() throws Exception {
    // This is what infra/docker-compose.yml greps for "UP", so it must keep working unchanged.
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
