package uz.academixai.infrastructure.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RlsTransactionFilter rlsTransactionFilter;
  private final AuthenticationEntryPointImpl authenticationEntryPoint;

  @Value("${academix.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  // Off by default: Swagger UI enumerates the full API surface, which a public production API
  // shouldn't hand out unauthenticated. Flip SWAGGER_PUBLIC=true per environment (e.g. for a
  // demo) to expose /swagger-ui/** and /v3/api-docs/** without a JWT.
  @Value("${academix.swagger-public:false}")
  private boolean swaggerPublic;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RlsTransactionFilter rlsTransactionFilter,
      AuthenticationEntryPointImpl authenticationEntryPoint) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rlsTransactionFilter = rlsTransactionFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt strength 12 — academix_backend_tdd.md
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    // Frontend (Next.js) and backend are different origins in local dev (:3000 vs :8080) —
    // confirmed necessary by real login requests silently never reaching the backend at all
    // without this. In production, Nginx proxies both under one host (see infra/nginx/nginx.conf)
    // so this matters less there, but local dev breaks completely without it.
    CorsConfiguration config = new CorsConfiguration();
    // Comma-separated so local dev (http://localhost:3000) and a deployed frontend
    // (e.g. a Vercel domain) can coexist:
    // FRONTEND_URL="http://localhost:3000,https://academix.vercel.app"
    config.setAllowedOrigins(
        java.util.Arrays.stream(frontendUrl.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true); // withCredentials: true on the frontend Axios client

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
        .authorizeHttpRequests(
            auth -> {
              // Only the genuinely public auth endpoints are open — profile/logout/change-password
              // must stay authenticated. A blanket "/api/v1/auth/**" permitAll (the old config)
              // silently exposed those to anonymous callers, where @AuthenticationPrincipal is null
              // and the controller NPE'd into a 500 instead of the proper 401 — confirmed by a real
              // role-access test run.
              auth.requestMatchers(
                      "/api/v1/auth/login",
                      "/api/v1/auth/refresh",
                      "/api/v1/auth/forgot-password",
                      "/api/v1/auth/reset-password",
                      "/api/v1/onboarding/status",
                      "/api/v1/onboarding/initial-setup",
                      // Not just "/actuator/health": Spring Security matches these exactly, so the
                      // probe paths underneath stayed authenticated — an orchestrator calling
                      // /actuator/health/liveness would have received a 401 and concluded the
                      // instance was unhealthy. Details stay hidden (show-details defaults to
                      // never), so nothing beyond UP/DOWN is revealed.
                      "/actuator/health",
                      "/actuator/health/liveness",
                      "/actuator/health/readiness")
                  .permitAll();
              if (swaggerPublic) {
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll();
              }
              auth.anyRequest().authenticated();
            })
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(rlsTransactionFilter, JwtAuthenticationFilter.class);
    return http.build();
  }
}
