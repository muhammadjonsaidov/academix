package uz.academixai.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Report;
import uz.academixai.domain.ReportType;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.StudentProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.SubjectProgressRow;
import uz.academixai.infrastructure.persistence.ReportEntity;
import uz.academixai.infrastructure.persistence.ReportRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.reports.JasperReportGenerator;
import uz.academixai.infrastructure.reports.ReportRow;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.interfaces.web.ApiException;

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
  private final GradeRepository gradeRepository;
  private final SchoolRepository schoolRepository;
  private final SchoolClassRepository classRepository;
  private final UserRepository userRepository;
  private final JasperReportGenerator reportGenerator;
  private final FileStorageService fileStorageService;

  public ReportService(
      ReportRepository reportRepository,
      GradeRepository gradeRepository,
      SchoolRepository schoolRepository,
      SchoolClassRepository classRepository,
      UserRepository userRepository,
      JasperReportGenerator reportGenerator,
      FileStorageService fileStorageService) {
    this.reportRepository = reportRepository;
    this.gradeRepository = gradeRepository;
    this.schoolRepository = schoolRepository;
    this.classRepository = classRepository;
    this.userRepository = userRepository;
    this.reportGenerator = reportGenerator;
    this.fileStorageService = fileStorageService;
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
    fileStorageService.upload(key, pdf, "application/pdf");

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
    byte[] content = fileStorageService.download(report.fileUrl());
    String fileName = "%s-%s.pdf".formatted(report.type(), report.quarter());
    return new ReportDownload(fileName, content);
  }

  private byte[] generateSchoolReportPdf(UUID schoolId, String quarter) {
    String schoolName =
        schoolRepository.findById(schoolId).map(s -> s.toDomain().name()).orElse("");
    List<ClassProgressRow> classes = gradeRepository.classProgress(schoolId, null, windowSince());
    List<ReportRow> rows =
        classes.stream()
            .map(
                c ->
                    new ReportRow(
                        c.getClassName(),
                        "%.1f%%".formatted(c.getAvgScore()),
                        String.valueOf(c.getGradedCount()),
                        String.valueOf(c.getStudentCount())))
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
    SchoolClassEntity schoolClass =
        classRepository
            .findByIdAndSchoolId(classId, schoolId)
            .orElseThrow(ReportService::classNotFound);
    List<StudentProgressRow> students =
        gradeRepository.classStudentProgress(schoolId, classId, windowSince());
    List<ReportRow> rows =
        students.stream()
            .map(
                s ->
                    new ReportRow(
                        s.getFirstName() + " " + s.getLastName(),
                        "%.1f%%".formatted(s.getAvgScore()),
                        String.valueOf(s.getGradedCount()),
                        String.valueOf(s.getTotalXp())))
            .toList();
    return reportGenerator.generatePdf(
        "Sinf chorak hisoboti",
        schoolClass.toDomain().fullName() + " — " + quarter,
        LocalDateTime.now().format(DISPLAY_FORMAT),
        "O'rtacha ball",
        "Baholangan soni",
        "Jami XP",
        rows);
  }

  private byte[] generateStudentReportPdf(UUID schoolId, UUID studentUserId, String quarter) {
    UserEntity student =
        userRepository.findById(studentUserId).orElseThrow(ReportService::studentNotFound);
    List<SubjectProgressRow> subjects =
        gradeRepository.studentSubjectProgress(schoolId, studentUserId, windowSince());
    List<ReportRow> rows =
        subjects.stream()
            .map(
                s ->
                    new ReportRow(
                        s.getSubjectName(),
                        "%.1f%%".formatted(s.getAvgScore()),
                        String.valueOf(s.getGradedCount()),
                        ""))
            .toList();
    return reportGenerator.generatePdf(
        "O'quvchi chorak hisoboti",
        student.toDomain().firstName() + " " + student.toDomain().lastName() + " — " + quarter,
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
