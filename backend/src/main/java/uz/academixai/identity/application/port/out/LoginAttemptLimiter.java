package uz.academixai.identity.application.port.out;

import java.time.Duration;

/** Distributed login-attempt counter used to slow credential-stuffing attacks. */
public interface LoginAttemptLimiter {

  boolean isBlocked(String key);

  long record(String key, Duration window);

  void reset(String key);

  void block(String key, Duration duration);
}
