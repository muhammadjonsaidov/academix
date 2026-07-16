package uz.academixai.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.academixai.infrastructure.persistence.HandwritingProfileEntity;
import uz.academixai.infrastructure.persistence.HandwritingProfileRepository;
import uz.academixai.infrastructure.persistence.HandwritingResetLogEntity;
import uz.academixai.infrastructure.persistence.HandwritingResetLogRepository;

/**
 * backend_tdd.md §6.5 "Handwriting Reset — Anomaliya Monitoring" — weekly, flags only, no
 * auto-block:
 *
 * <ol>
 *   <li>a student reset 2+ times this semester
 *   <li>a teacher whose reset-initiation count is a statistical outlier vs. other teachers
 * </ol>
 *
 * <p><b>Scope simplification (judgment call, see ROADMAP.md Sprint 5):</b> the spec's own words are
 * "faqat flagged holatlar admin dashboard da ko'rinadi" (only flagged cases show up on an admin
 * dashboard) — no such dashboard/table exists yet (admin analytics is explicitly deferred to Sprint
 * 6+ in ROADMAP.md). This logs flagged patterns at WARN and returns them, rather than inventing a
 * new persisted-flags table for a UI that doesn't exist to consume it yet. Also: {@code
 * reset_count_this_semester} has no semester-boundary reset job anywhere in this codebase (a
 * separate real gap, not built here) — "this semester" is read as "all-time" until that job exists,
 * which only makes this detector more conservative (never under-flags), not less correct.
 */
@Service
public class HandwritingAnomalyService {

  private static final Logger log = LoggerFactory.getLogger(HandwritingAnomalyService.class);
  private static final int STUDENT_RESET_FLAG_THRESHOLD = 2;
  private static final double OUTLIER_Z_SCORE = 2.0;

  private final HandwritingProfileRepository profileRepository;
  private final HandwritingResetLogRepository resetLogRepository;

  public HandwritingAnomalyService(
      HandwritingProfileRepository profileRepository,
      HandwritingResetLogRepository resetLogRepository) {
    this.profileRepository = profileRepository;
    this.resetLogRepository = resetLogRepository;
  }

  public record AnomalyReport(List<UUID> flaggedStudents, List<UUID> flaggedTeachers) {}

  // Sunday 02:00, matching backend_tdd.md §6.5's literal schedule.
  @Scheduled(cron = "0 0 2 * * SUN")
  public void detectHandwritingResetAnomalies() {
    AnomalyReport report = computeAnomalies();
    if (!report.flaggedStudents().isEmpty()) {
      log.warn(
          "Handwriting reset anomaly: {} student(s) reset 2+ times: {}",
          report.flaggedStudents().size(),
          report.flaggedStudents());
    }
    if (!report.flaggedTeachers().isEmpty()) {
      log.warn(
          "Handwriting reset anomaly: {} teacher(s) are statistical outliers in reset count: {}",
          report.flaggedTeachers().size(),
          report.flaggedTeachers());
    }
  }

  public AnomalyReport computeAnomalies() {
    List<UUID> flaggedStudents =
        profileRepository.findAll().stream()
            .filter(p -> p.getResetCountThisSemester() >= STUDENT_RESET_FLAG_THRESHOLD)
            .map(HandwritingProfileEntity::getStudentId)
            .toList();

    Map<UUID, Long> resetsByTeacher =
        resetLogRepository.findAll().stream()
            .map(HandwritingResetLogEntity::toDomain)
            .collect(Collectors.groupingBy(entry -> entry.teacherId(), Collectors.counting()));

    List<UUID> flaggedTeachers = outlierTeachers(resetsByTeacher);

    return new AnomalyReport(flaggedStudents, flaggedTeachers);
  }

  /**
   * Flags teachers whose reset count is more than {@link #OUTLIER_Z_SCORE} std-devs above the mean.
   */
  private static List<UUID> outlierTeachers(Map<UUID, Long> resetsByTeacher) {
    if (resetsByTeacher.size() < 2) {
      // Can't call anyone an "outlier" relative to nobody else.
      return List.of();
    }
    double mean = resetsByTeacher.values().stream().mapToLong(Long::longValue).average().orElse(0);
    double variance =
        resetsByTeacher.values().stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average()
            .orElse(0);
    double stdDev = Math.sqrt(variance);
    if (stdDev == 0) {
      return List.of();
    }
    return resetsByTeacher.entrySet().stream()
        .filter(e -> (e.getValue() - mean) / stdDev > OUTLIER_Z_SCORE)
        .map(Map.Entry::getKey)
        .toList();
  }
}
