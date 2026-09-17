package uz.academixai.identity.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound port for the audit trail of teacher-assisted student resets.
 *
 * <p>Written even though nothing reads it yet: the point of the log is to answer "who reset this
 * student's password" after the fact, and a trail that only exists once a screen needs it is not a
 * trail.
 */
public interface PasswordResetLogStore {

  void record(UUID schoolId, UUID studentId, UUID teacherId, LocalDateTime at);
}
