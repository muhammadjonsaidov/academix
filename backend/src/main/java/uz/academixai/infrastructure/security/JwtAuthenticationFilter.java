package uz.academixai.infrastructure.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads {@code Authorization: Bearer <token>}, verifies it, and populates the security context. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String token = null;
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      token = header.substring(7);
    } else if (request.getRequestURI().startsWith("/api/v1/realtime/")) {
      // EventSource cannot set Authorization headers, so the SSE endpoint accepts the JWT as a
      // query param instead. Scoped to /api/v1/realtime/** ONLY (query strings can end up in
      // access logs) and uses the short-lived access token (15 min), never the refresh token.
      token = request.getParameter("token");
    }
    if (token != null) {
      try {
        var claims = jwtService.parseAccessToken(token);
        var principal = new AcademixPrincipal(claims.userId(), claims.role(), claims.schoolId());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException | IllegalArgumentException e) {
        // Invalid/expired token — leave unauthenticated, let the security chain 401 downstream
        // endpoints that require auth. Don't fail the whole request here.
      }
    }
    filterChain.doFilter(request, response);
  }
}
