package uz.academixai.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import uz.academixai.TestcontainersConfiguration;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.JwtService;
import uz.academixai.wellbeing.adapter.in.scheduling.BehaviorAnalysisScheduler;

/**
 * The psychologist module is a paid add-on and is off by default ({@code
 * academix.features.psychology.enabled=false}). Off must mean genuinely absent: no nightly AI
 * analysis, no psychologist/teacher-signal/admin-psychologist endpoints, and psychologist accounts
 * cannot sign in to an empty dashboard. {@link RoleAccessIntegrationTest} covers the module's
 * endpoints with the flag switched on.
 */
@Import(TestcontainersConfiguration.class)
// Pinned rather than relying on the default, so an exported ACADEMIX_FEATURE_PSYCHOLOGY=true in a
// developer shell or CI can't flip what this test proves.
@SpringBootTest(properties = "academix.features.psychology.enabled=false")
@AutoConfigureMockMvc
class PsychologyModuleDisabledIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ApplicationContext context;
  @Autowired private JwtService jwtService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void nightlyBehaviorAnalysisIsNotScheduled() {
    assertThat(context.getBeanNamesForType(BehaviorAnalysisScheduler.class)).isEmpty();
  }

  @Test
  void psychologistEndpointsAreNotRegistered() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/psychologist/dashboard")
                .header(HttpHeaders.AUTHORIZATION, as(Role.PSYCHOLOGIST)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ERR_NOT_FOUND"));
  }

  @Test
  void teacherSignalEndpointsAreNotRegistered() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/teacher/psychological-signals")
                .header(HttpHeaders.AUTHORIZATION, as(Role.TEACHER)))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCannotManagePsychologists() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/psychologists").header(HttpHeaders.AUTHORIZATION, as(Role.ADMIN)))
        .andExpect(status().isNotFound());
  }

  @Test
  void notificationSettingsOmitPsychologicalAlerts() throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/v1/notifications/preferences")
                    .header(HttpHeaders.AUTHORIZATION, as(Role.TEACHER)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).contains("HOMEWORK_GRADED").doesNotContain("PSYCHOLOGICAL_ALERT");
  }

  @Test
  void psychologistCannotSignIn() throws Exception {
    String phone = "+998900007777";
    String password = "password123";
    userRepository.save(
        new UserEntity(
            UUID.randomUUID(),
            "Psix",
            "Olog",
            phone,
            null,
            passwordEncoder.encode(password),
            Role.PSYCHOLOGIST,
            true,
            LocalDateTime.now(),
            null,
            null));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ERR_MODULE_DISABLED"));
  }

  private String as(Role role) {
    return "Bearer " + jwtService.issueAccessToken(UUID.randomUUID(), role, UUID.randomUUID());
  }
}
