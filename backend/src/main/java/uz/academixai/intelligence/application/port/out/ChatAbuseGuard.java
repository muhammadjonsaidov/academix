package uz.academixai.intelligence.application.port.out;

import java.util.UUID;

/** Per-student rate-limit policy boundary for tutor chat. */
public interface ChatAbuseGuard {

  void enforce(UUID studentId);
}
