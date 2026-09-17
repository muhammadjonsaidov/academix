package uz.academixai.reporting.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.reporting.application.port.out.GradeStatistics;
import uz.academixai.reporting.application.port.out.ReportDirectoryLookup;
import uz.academixai.reporting.application.port.out.ReportFileStore;
import uz.academixai.reporting.domain.Report;
import uz.academixai.reporting.domain.ReportType;
import uz.academixai.reporting.infrastructure.pdf.JasperReportGenerator;
import uz.academixai.reporting.infrastructure.pdf.ReportRow;
import uz.academixai.reporting.infrastructure.persistence.ReportEntity;
import uz.academixai.reporting.infrastructure.persistence.ReportRepository;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md's {@code ReportService} pseudocode ({@code generateStudentReport}/{@code
 * generateClassReport}/{@code generateSchoolReport} + {@code generatePDF}) — no window definition
 * exists for {@code quarter} anywhere in any spec doc, so it's treated as an opaque display label
 * (e.g. "2025-2026-1"), not parsed into a date range; the underlying grade query instead reuses
 * {@code AdminAnalyticsService}'s "quarter" lookback window (~90 days — a quarter is 1/4 of the
 * academic year, not a semester's 1/2), same judgment call. PDF is generated synchronously in the
 * request (no queue) — report generation is a single admin-triggered action over an already-small
 * dataset, not a per-student fan-out like homework grading, so the async/queue machinery elsewhere
 * in this codebase doesn't apply here.
 */
@Service
public class ReportService {

  private static final int QUARTER_WINDOW_DAYS = 90;
  private static final DateTimeFormatter DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final ReportRepository reportRepository;
  private final GradeStatistics grades;
  private final ReportDirectoryLookup directory;
  private final JasperReportGenerator reportGenerator;
  private final ReportFileStore files;

  public ReportService(
      ReportRepository reportRepository,
      GradeStatistics grades,
      ReportDirectoryLookup directory,
      JasperReportGenerator reportGenerator,
      ReportFileStore files) {
    this.reportRepository = reportRepository;
    this.grades = grades;
    this.directory = directory;
    this.reportGenerator = reportGenerator;
    this.files = files;
  }

  public Report generate(
      UUID schoolId, ReportType type, String quarter, UUID targetId, UUID generatedByUserId) {
    byte[] pdf =
        switch (type) {
          case SCHOOL -> generateSchoolReportPdf(schoolId, quarter);
          case CLASS -> generateClassReportPdf(schoolId, requireTargetId(targetId), quarter);
          case STUDENT -> generateStudentReportPdf(schoolId, requireTargetId(targetId), quarter);
        };

    String key = "reports/%s/%s.pdf".formatted(schoolId, UUID.randomUUID());
    files.upload(key, pdf, "application/pdf");

    Report report =
        new Report(
            UUID.randomUUID(),
            schoolId,
            type,
            quarter,
            targetId,
            key,
            generatedByUserId,
            LocalDateTime.now());
    return reportRepository.save(ReportEntity.fromDomain(report)).toDomain();
  }

  /**
   * Newest report of this type for a target, if one was generated within {@code since}.
   *
   * <p>Published for Family's parent-facing quarter report, which reuses a fresh report instead of
   * asking JasperReports to rebuild an identical PDF on every page view. Family must not read
   * Reporting's tables to answer that question, so the rule lives here.
   */
  public Optional<Report> findRecent(
      UUID schoolId, ReportType type, UUID targetId, LocalDateTime since) {
    return reportRepository
        .findFirstByTypeAndTargetIdAndGeneratedAtAfterOrderByGeneratedAtDesc(type, targetId, since)
        .map(ReportEntity::toDomain)
        .filter(report -> report.schoolId().equals(schoolId));
  }

  public List<Report> list(UUID schoolId) {
    return reportRepository.findBySchoolIdOrderByGeneratedAtDesc(schoolId).stream()
        .map(ReportEntity::toDomain)
        .toList();
  }

  public record ReportDownload(String fileName, byte[] content) {}

  public ReportDownload download(UUID schoolId, UUID reportId) {
    Report report =
        reportRepository
            .findByIdAndSchoolId(reportId, schoolId)
            .map(ReportEntity::toDomain)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_REPORT_NOT_FOUND",
                        "Hisobot topilmadi.",
                        "ID ni tekshiring yoki ro'yxatni yangilang."));
    byte[] content = files.download(report.fileUrl());
    String fileName = "%s-%s.pdf".formatted(report.type(), report.quarter());
    return new ReportDownload(fileName, content);
  }

  private byte[] generateSchoolReportPdf(UUID schoolId, String quarter) {
    String schoolName = directory.schoolName(schoolId).orElse("");
    List<GradeStatistics.ClassRow> classes = grades.classProgress(schoolId, windowSince());
    List<ReportRow> rows =
        classes.stream()
            .map(
                c ->
                    new ReportRow(
                        c.className(),
                        "%.1f%%".formatted(c.avgScore()),
                        String.valueOf(c.gradedCount()),
                        String.valueOf(c.studentCount())))
            .toList();
    return reportGenerator.generatePdf(
        "Maktab chorak hisoboti",
        schoolName + " — " + quarter,
        LocalDateTime.now().format(DISPLAY_FORMAT),
        "O'rtacha ball",
        "Baholangan soni",
        "O'quvchilar soni",
        rows);
  }

  private byte[] generateClassReportPdf(UUID schoolId, UUID classId, String quarter) {
    String classLabel =
        directory.classLabel(classId, schoolId).orElseThrow(ReportService::classNotFound);
    List<GradeStatistics.StudentRow> students =
        grades.classStudentProgress(schoolId, classId, windowSince());
    List<ReportRow> rows =
        students.stream()
            .map(
                s ->
                    new ReportRow(
                        s.firstName() + " " + s.lastName(),
                        "%.1f%%".formatted(s.avgScore()),
                        String.valueOf(s.gradedCount()),
                        String.valueOf(s.totalXp())))
            .toList();
    return reportGenerator.generatePdf(
        "Sinf chorak hisoboti",
        classLabel + " — " + quarter,
        LocalDateTime.now().format(DISPLAY_FORMAT),
        "O'rtacha ball",
        "Baholangan soni",
        "Jami XP",
        rows);
  }

  private byte[] generateStudentReportPdf(UUID schoolId, UUID studentUserId, String quarter) {
    String studentName =
        directory.studentFullName(studentUserId).orElseThrow(ReportService::studentNotFound);
    List<GradeStatistics.SubjectRow> subjects =
        grades.studentSubjectProgress(schoolId, studentUserId, windowSince());
    List<ReportRow> rows =
        subjects.stream()
            .map(
                s ->
                    new ReportRow(
                        s.subjectName(),
                        "%.1f%%".formatted(s.avgScore()),
                        String.valueOf(s.gradedCount()),
                        ""))
            .toList();
    return reportGenerator.generatePdf(
        "O'quvchi chorak hisoboti",
        studentName + " — " + quarter,
        LocalDateTime.now().format(DISPLAY_FORMAT),
        "O'rtacha ball",
        "Baholangan soni",
        "",
        rows);
  }

  private LocalDateTime windowSince() {
    return LocalDateTime.now().minusDays(QUARTER_WINDOW_DAYS);
  }

  private static UUID requireTargetId(UUID targetId) {
    if (targetId == null) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_TARGET_ID_REQUIRED",
          "targetId talab qilinadi.",
          "CLASS/STUDENT turidagi hisobot uchun targetId yuboring.");
    }
    return targetId;
  }

  private static ApiException classNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_CLASS_NOT_FOUND",
        "Sinf topilmadi.",
        "ID ni tekshiring yoki ro'yxatni yangilang.");
  }

  private static ApiException studentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_STUDENT_NOT_FOUND",
        "O'quvchi topilmadi.",
        "ID ni tekshiring yoki ro'yxatni yangilang.");
  }
}
