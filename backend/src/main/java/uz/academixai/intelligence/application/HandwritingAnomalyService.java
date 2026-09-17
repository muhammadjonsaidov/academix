package uz.academixai.intelligence.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.academixai.intelligence.application.port.out.HandwritingResetAudit;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * backend_tdd.md §6.5 "Handwriting Reset — Anomaliya Monitoring" — weekly, flags only, no
 * auto-block:
 *
 * <ol>
 *   <li>a student reset 2+ times this quarter
 *   <li>a teacher whose reset-initiation count is a statistical outlier vs. other teachers
 * </ol>
 *
 * <p><b>Scope simplification (judgment call, see ROADMAP.md Sprint 5):</b> the spec's own words are
 * "faqat flagged holatlar admin dashboard da ko'rinadi" (only flagged cases show up on an admin
 * dashboard) — no such dashboard/table exists yet (admin analytics is explicitly deferred to Sprint
 * 6+ in ROADMAP.md). This logs flagged patterns at WARN and returns them, rather than inventing a
 * new persisted-flags table for a UI that doesn't exist to consume it yet. Also: {@code
 * reset_count_this_quarter} has no quarter-boundary reset job anywhere in this codebase (a separate
 * real gap, not built here) — "this quarter" is read as "all-time" until that job exists, which
 * only makes this detector more conservative (never under-flags), not less correct.
 *
 * <p><b>Runs as the system role.</b> This is a system-wide audit — flagging a teacher as a
 * statistical outlier only means anything against every teacher, so the job is cross-tenant by
 * definition and has no schoolId it could be scoped to. Before this went through {@link
 * TenantScope} the read of the RLS-protected {@code handwriting_reset_logs} ran with no scope at
 * all: it worked under the test suite (Testcontainers connects as a superuser, so RLS is inert) and
 * would have failed on the real restricted role the app uses. Per-school statistics — a more
 * meaningful outlier set, since a teacher is only comparable to colleagues in the same school — is
 * a deliberate follow-up, not a silent omission.
 *
 * <p>Moved here from the legacy {@code application} package, next to the handwriting model it
 * audits. The two repository reads became {@link HandwritingResetAudit}: the audit's own statistics
 * (mean, variance, the z-score cut) stay here, where they can be read alongside the rule they
 * implement.
 */
@Service
public class HandwritingAnomalyService {

  private static final Logger log = LoggerFactory.getLogger(HandwritingAnomalyService.class);
  private static final int STUDENT_RESET_FLAG_THRESHOLD = 2;
  private static final double OUTLIER_Z_SCORE = 2.0;

  private final HandwritingResetAudit audit;
  private final TenantScope tenantScope;

  public HandwritingAnomalyService(HandwritingResetAudit audit, TenantScope tenantScope) {
    this.audit = audit;
    this.tenantScope = tenantScope;
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
    return tenantScope.callAsSystem(this::computeAnomaliesInSystemScope);
  }

  private AnomalyReport computeAnomaliesInSystemScope() {
    List<UUID> flaggedStudents = audit.studentIdsWithAtLeastResets(STUDENT_RESET_FLAG_THRESHOLD);

    Map<UUID, Long> resetsByTeacher =
        audit.initiatingTeacherIds().stream()
            .collect(Collectors.groupingBy(teacherId -> teacherId, Collectors.counting()));

    return new AnomalyReport(flaggedStudents, outlierTeachers(resetsByTeacher));
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
