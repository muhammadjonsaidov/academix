package uz.academixai.intelligence.infrastructure.legacy;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.HandwritingProfileEntity;
import uz.academixai.infrastructure.persistence.HandwritingProfileRepository;
import uz.academixai.infrastructure.persistence.HandwritingResetLogEntity;
import uz.academixai.infrastructure.persistence.HandwritingResetLogRepository;
import uz.academixai.intelligence.application.port.out.HandwritingResetAudit;

/** Adapter for {@link HandwritingResetAudit} over the legacy handwriting repositories. */
@Component
public class LegacyHandwritingResetAudit implements HandwritingResetAudit {

  private final HandwritingProfileRepository profiles;
  private final HandwritingResetLogRepository resetLogs;

  public LegacyHandwritingResetAudit(
      HandwritingProfileRepository profiles, HandwritingResetLogRepository resetLogs) {
    this.profiles = profiles;
    this.resetLogs = resetLogs;
  }

  @Override
  public List<UUID> studentIdsWithAtLeastResets(int minResets) {
    return profiles.findAll().stream()
        .filter(profile -> profile.getResetCountThisQuarter() >= minResets)
        .map(HandwritingProfileEntity::getStudentId)
        .toList();
  }

  @Override
  public List<UUID> initiatingTeacherIds() {
    return resetLogs.findAll().stream()
        .map(HandwritingResetLogEntity::toDomain)
        .map(entry -> entry.teacherId())
        .toList();
  }
}
