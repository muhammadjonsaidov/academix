package uz.academixai.family.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.family.application.port.in.ParentChildAccess;
import uz.academixai.family.application.port.out.ChildReadModel;
import uz.academixai.reporting.application.ReportService;
import uz.academixai.reporting.application.ReportService.ReportDownload;
import uz.academixai.reporting.domain.Report;
import uz.academixai.reporting.domain.ReportType;

/**
 * academix_tz.md §2.5 {@code GET /parent/children/{studentId}/quarter-report} + {@code
 * .../download} — no response shape or "generate vs fetch" semantics are given (unlike {@code POST
 * /admin/reports/generate}'s explicit verb). Judgment call: get-or-generate — reuse a STUDENT-type
 * report generated for this child in the last 24h if one exists (avoids regenerating an identical
 * PDF on every page view), otherwise generate a fresh one for a default "joriy" (current) quarter
 * label. Reuses Reporting's published pipeline end-to-end, authorized via Family's {@link
 * ParentChildAccess} instead of an admin role.
 *
 * <p>Both the "is there a fresh report" question and the school lookup go through published APIs
 * (Reporting's {@code findRecent}, Family's read model) — Family never reads Reporting's tables.
 */
@Service
public class ParentReportService {

  private static final int REUSE_WINDOW_HOURS = 24;
  private static final String DEFAULT_QUARTER = "joriy";

  private final ParentChildAccess parentLinks;
  private final ChildReadModel children;
  private final ReportService reportService;

  public ParentReportService(
      ParentChildAccess parentLinks, ChildReadModel children, ReportService reportService) {
    this.parentLinks = parentLinks;
    this.children = children;
    this.reportService = reportService;
  }

  public Report quarterReport(UUID parentUserId, UUID studentId) {
    parentLinks.requireLinkedChild(parentUserId, studentId);
    UUID schoolId = resolveSchoolId(studentId);

    return reportService
        .findRecent(
            schoolId,
            ReportType.STUDENT,
            studentId,
            LocalDateTime.now().minusHours(REUSE_WINDOW_HOURS))
        .orElseGet(
            () ->
                reportService.generate(
                    schoolId, ReportType.STUDENT, DEFAULT_QUARTER, studentId, parentUserId));
  }

  public ReportDownload download(UUID parentUserId, UUID studentId) {
    Report report = quarterReport(parentUserId, studentId);
    return reportService.download(report.schoolId(), report.id());
  }

  private UUID resolveSchoolId(UUID studentId) {
    return children.profile(studentId).map(profile -> profile.schoolId()).orElseThrow();
  }
}
