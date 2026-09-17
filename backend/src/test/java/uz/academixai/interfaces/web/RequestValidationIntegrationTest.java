package uz.academixai.interfaces.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uz.academixai.TestcontainersConfiguration;

/**
 * Bean validation at the HTTP boundary, on the public (unauthenticated) endpoints so the assertions
 * never depend on a token or on seed data.
 *
 * <p>Before this existed, every request DTO was a bare record: {@code POST /api/v1/auth/login} with
 * an empty body reached {@code AuthenticationService} with a null identifier, and an over-long
 * value travelled all the way to Postgres, where it failed as a 500 rather than a 400. The shape of
 * the rejection matters as much as the status — validation answers with the same {@code
 * ERR_VALIDATION} code and {@code ApiErrorResponse} body {@code PasswordPolicy} already used, so a
 * client sees one validation contract regardless of which layer caught the mistake.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RequestValidationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static final String LOGIN = "/api/v1/auth/login";
  private static final String INITIAL_SETUP = "/api/v1/onboarding/initial-setup";

  @Test
  void blankLoginBodyIsRejectedBeforeTheServiceRuns() throws Exception {
    mockMvc
        .perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_VALIDATION"))
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void identifierLongerThanItsColumnIsRejected() throws Exception {
    String tooLong = "x".repeat(101);
    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + tooLong + "\",\"password\":\"Password1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_VALIDATION"));
  }

  @Test
  void malformedJsonIsAClientErrorNotAServerError() throws Exception {
    mockMvc
        .perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_VALIDATION"));
  }

  @Test
  void initialSetupRequiresEverySignupField() throws Exception {
    mockMvc
        .perform(
            post(INITIAL_SETUP)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ali\",\"lastName\":\"Valiyev\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_VALIDATION"));
  }

  @Test
  void aWellFormedLoginStillReachesTheService() throws Exception {
    // The point of the whole slice: validation rejects shape, not content. A correctly shaped
    // request with unknown credentials must still be answered by the authentication service (401),
    // never short-circuited as a 400 by the validator.
    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"+998900111222\",\"password\":\"Password1\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("ERR_INVALID_CREDENTIALS"));
  }
}
