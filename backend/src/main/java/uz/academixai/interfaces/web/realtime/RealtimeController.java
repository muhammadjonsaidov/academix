package uz.academixai.interfaces.web.realtime;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uz.academixai.infrastructure.realtime.RealtimeEventBus;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * {@code GET /api/v1/realtime/events} — long-lived SSE stream, one per browser tab. The frontend
 * connects via {@code EventSource} and passes the JWT as a {@code ?token=} query param (EventSource
 * cannot set an {@code Authorization} header; {@code JwtAuthenticationFilter} accepts the query
 * param on this path only — short-lived access token, scoped trade-off).
 *
 * <p>SecurityConfig's {@code anyRequest().authenticated()} already gates this endpoint; the {@code
 * principal == null} branch is belt-and-braces for a hypothetical filter bypass and just completes
 * the stream instead of erroring.
 */
@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeController {

  private final RealtimeEventBus realtimeEventBus;

  public RealtimeController(RealtimeEventBus realtimeEventBus) {
    this.realtimeEventBus = realtimeEventBus;
  }

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@AuthenticationPrincipal AcademixPrincipal principal) {
    if (principal == null) {
      SseEmitter emitter = new SseEmitter();
      emitter.complete();
      return emitter;
    }
    return realtimeEventBus.subscribe(principal.userId());
  }
}
