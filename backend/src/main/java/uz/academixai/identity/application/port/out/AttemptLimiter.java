package uz.academixai.identity.application.port.out;

import java.time.Duration;

/**
 * Distributed keyed attempt counter.
 *
 * <p>Named for the mechanism rather than one caller: Identity uses it to slow credential-stuffing
 * on login ({@code login_block:} keys) and email bombing on password reset ({@code
 * password_reset_rate:} keys). Both are "count attempts per key in a window, then refuse", which is
 * why one port covers them instead of two near-identical ones.
 */
public interface AttemptLimiter {

  boolean isBlocked(String key);

  long record(String key, Duration window);

  void reset(String key);

  void block(String key, Duration duration);
}
