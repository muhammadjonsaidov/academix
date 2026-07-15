package uz.academixai.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import uz.academixai.interfaces.web.ApiErrorResponse;

/**
 * Spring Security's default (no entry point configured) returns a bare 403 for missing/invalid
 * credentials, not our documented error shape — this fixes that. Real authorization failures (valid
 * token, wrong role) still get a normal 403 from @PreAuthorize.
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    var body =
        new ApiErrorResponse(
            401,
            "ERR_EXPIRED_TOKEN",
            "Seans muddati tugadi. Tizimga qayta kiring.",
            "Client refresh token orqali access token olishi kerak.");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
