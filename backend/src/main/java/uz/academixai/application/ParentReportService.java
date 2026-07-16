package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.application.ReportService.ReportDownload;
import uz.academixai.domain.Report;
import uz.academixai.domain.ReportType;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.ReportEntity;
import uz.academixai.infrastructure.persistence.ReportRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;

/**
 * academix_tz.md §2.5 {@code GET /parent/children/{studentId}/semester-report} + {@code
 * .../download} — no response shape or "generate vs fetch" semantics are given (unlike {@code POST
 * /admin/reports/generate}'s explicit verb). Judgment call: get-or-generate — reuse a STUDENT-type
 * report generated for this child in the last 24h if one exists (avoids regenerating an identical
 * PDF on every page view), otherwise generate a fresh one for a default "joriy" (current) semester
 * label. Reuses {@link ReportService} end-to-end (same PDF pipeline as admin reports), authorized
 * via {@link ParentLinkService} instead of admin role.
 */
@Service
public class ParentReportService {

  private static final int REUSE_WINDOW_HOURS = 24;
  private static final String DEFAULT_SEMESTER = "joriy";

  private final ParentLinkService parentLinkService;
  private final StudentProfileRepository studentProfileRepository;
  private final ReportRepository reportRepository;
  private final ReportService reportService;

  public ParentReportService(
      ParentLinkService parentLinkService,
      StudentProfileRepository studentProfileRepository,
      ReportRepository reportRepository,
      ReportService reportService) {
    this.parentLinkService = parentLinkService;
    this.studentProfileRepository = studentProfileRepository;
    this.reportRepository = reportRepository;
    this.reportService = reportService;
  }

  public Report semesterReport(UUID parentUserId, UUID studentId) {
    parentLinkService.requireLinkedChild(parentUserId, studentId);
    UUID schoolId = resolveSchoolId(studentId);

    return reportRepository
        .findFirstByTypeAndTargetIdAndGeneratedAtAfterOrderByGeneratedAtDesc(
            ReportType.STUDENT, studentId, LocalDateTime.now().minusHours(REUSE_WINDOW_HOURS))
        .map(ReportEntity::toDomain)
        .orElseGet(
            () ->
                reportService.generate(
                    schoolId, ReportType.STUDENT, DEFAULT_SEMESTER, studentId, parentUserId));
  }

  public ReportDownload download(UUID parentUserId, UUID studentId) {
    Report report = semesterReport(parentUserId, studentId);
    return reportService.download(report.schoolId(), report.id());
  }

  private UUID resolveSchoolId(UUID studentId) {
    return studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::toDomain)
        .map(StudentProfile::schoolId)
        .orElseThrow();
  }
}
