package uz.academixai.identity.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.PasswordResetLog;
import uz.academixai.identity.application.port.out.PasswordResetLogStore;
import uz.academixai.infrastructure.persistence.PasswordResetLogEntity;
import uz.academixai.infrastructure.persistence.PasswordResetLogRepository;

/** Adapter for {@link PasswordResetLogStore} over the legacy reset-log repository. */
@Repository
public class JpaPasswordResetLogStore implements PasswordResetLogStore {

  private final PasswordResetLogRepository logs;

  public JpaPasswordResetLogStore(PasswordResetLogRepository logs) {
    this.logs = logs;
  }

  @Override
  public void record(UUID schoolId, UUID studentId, UUID teacherId, LocalDateTime at) {
    logs.save(
        PasswordResetLogEntity.fromDomain(
            new PasswordResetLog(UUID.randomUUID(), schoolId, studentId, teacherId, at)));
  }
}
