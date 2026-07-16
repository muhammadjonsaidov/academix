package uz.academixai.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.domain.ExamGrade;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackRepository;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamGradeEntity;
import uz.academixai.infrastructure.persistence.ExamGradeRepository;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Nazorat ishi" — bulk image upload, submission listing/grading, and
 * approve-all over {@code exam_submissions}.
 */
@Service
public class ExamSubmissionService {

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
      List.of("image/jpeg", "image/png");

  private final ExamRepository examRepository;
  private final ExamSubmissionRepository submissionRepository;
  private final ExamAIFeedbackRepository feedbackRepository;
  private final ExamGradeRepository gradeRepository;
  private final UserRepository userRepository;
  private final FileStorageService fileStorageService;
  private final ExamSubmissionWriter submissionWriter;

  public ExamSubmissionService(
      ExamRepository examRepository,
      ExamSubmissionRepository submissionRepository,
      ExamAIFeedbackRepository feedbackRepository,
      ExamGradeRepository gradeRepository,
      UserRepository userRepository,
      FileStorageService fileStorageService,
      ExamSubmissionWriter submissionWriter) {
    this.examRepository = examRepository;
    this.submissionRepository = submissionRepository;
    this.feedbackRepository = feedbackRepository;
    this.gradeRepository = gradeRepository;
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
    this.submissionWriter = submissionWriter;
  }

  public record BulkUploadResult(int queued) {}

  /**
   * {@code images}/{@code studentIds} match positionally (academix_tz.md §2.3). Each submission is
   * its own {@code REQUIRES_NEW} unit of work — {@code RlsTransactionFilter} wraps the whole HTTP
   * request in one transaction, so without this a single bad file in a 30-paper batch would mark
   * the entire request rollback-only (the exact partial-success trap documented in CLAUDE.md for
   * {@code StudentManagementService.create()}). Unlike that case, {@code exam_submissions} IS
   * RLS-enabled, so each {@code REQUIRES_NEW} call must re-run its own {@code SET LOCAL} — a fresh
   * physical transaction gets a different pooled connection, which has no session variable set.
   */
  public BulkUploadResult bulkUpload(
      UUID schoolId,
      UUID teacherId,
      UUID examId,
      List<MultipartFile> images,
      List<UUID> studentIds) {
    requireOwnedExam(schoolId, teacherId, examId);
    if (images.size() != studentIds.size()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_FILE",
          "images va studentIds soni mos kelmadi.",
          "Har bir rasm uchun bitta studentId yuboring.");
    }

    int queued = 0;
    for (int i = 0; i < images.size(); i++) {
      MultipartFile image = images.get(i);
      UUID studentId = studentIds.get(i);
      validateImage(image);
      String imageUrl = uploadImage(schoolId, examId, studentId, image);
      submissionWriter.createSubmission(schoolId, examId, studentId, imageUrl);
      queued++;
    }
    return new BulkUploadResult(queued);
  }

  public record SubmissionWithFeedback(
      ExamSubmission submission,
      String studentName,
      ExamAIFeedbackEntity feedback,
      ExamGradeEntity grade) {}

  public List<SubmissionWithFeedback> list(UUID schoolId, UUID teacherId, UUID examId) {
    requireOwnedExam(schoolId, teacherId, examId);
    return submissionRepository.findByExamIdOrderByUploadedAtDesc(examId).stream()
        .map(ExamSubmissionEntity::toDomain)
        .map(this::toSubmissionWithFeedback)
        .toList();
  }

  private SubmissionWithFeedback toSubmissionWithFeedback(ExamSubmission submission) {
    return new SubmissionWithFeedback(
        submission,
        studentName(submission.studentId()),
        feedbackRepository.findByExamSubmissionId(submission.id()).orElse(null),
        gradeRepository.findByExamSubmissionId(submission.id()).orElse(null));
  }

  private String studentName(UUID studentId) {
    return userRepository
        .findById(studentId)
        .map(u -> u.getFirstName() + " " + u.getLastName())
        .orElse("");
  }

  public ExamGrade grade(
      UUID schoolId,
      UUID teacherId,
      UUID examId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment) {
    requireOwnedExam(schoolId, teacherId, examId);
    ExamSubmissionEntity subEntity = requireSubmission(examId, submissionId);

    ExamGradeEntity existing = gradeRepository.findByExamSubmissionId(submissionId).orElse(null);
    UUID gradeId = existing != null ? existing.toDomain().id() : UUID.randomUUID();
    ExamGrade domainGrade =
        new ExamGrade(
            gradeId,
            submissionId,
            teacherId,
            score,
            fivePointGrade,
            teacherComment,
            LocalDateTime.now());
    ExamGrade saved = gradeRepository.save(ExamGradeEntity.fromDomain(domainGrade)).toDomain();

    ExamSubmission submission = subEntity.toDomain();
    ExamSubmission graded =
        new ExamSubmission(
            submission.id(),
            submission.schoolId(),
            submission.examId(),
            submission.studentId(),
            submission.imageUrl(),
            SubmissionStatus.GRADED,
            submission.flaggedForReview(),
            submission.uploadedAt());
    submissionRepository.save(ExamSubmissionEntity.fromDomain(graded));
    return saved;
  }

  /** academix_tz.md §2.3 — only {@code flaggedForReview=false} submissions bulk-approve. */
  public int approveAll(UUID schoolId, UUID teacherId, UUID examId) {
    requireOwnedExam(schoolId, teacherId, examId);
    List<ExamSubmissionEntity> approvable =
        submissionRepository.findByExamIdOrderByUploadedAtDesc(examId).stream()
            .filter(s -> !s.isFlaggedForReview())
            .filter(s -> s.getStatus() != SubmissionStatus.GRADED)
            .toList();
    for (ExamSubmissionEntity subEntity : approvable) {
      ExamAIFeedbackEntity feedback =
          feedbackRepository.findByExamSubmissionId(subEntity.getId()).orElse(null);
      float aiScore = feedback == null ? 0f : feedback.toDomain().aiScorePercent();
      grade(
          schoolId,
          teacherId,
          examId,
          subEntity.getId(),
          Math.round(aiScore),
          scoreToFivePoint(aiScore),
          "AI baholovi avtomatik tasdiqlandi.");
    }
    return approvable.size();
  }

  private static int scoreToFivePoint(float score) {
    if (score >= 87) return 5;
    if (score >= 70) return 4;
    if (score >= 50) return 3;
    return 2;
  }

  private void validateImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_FILE",
          "Rasm fayli talab qilinadi.",
          "Rasm yuklang.");
    }
    if (image.getSize() > MAX_IMAGE_BYTES) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_FILE",
          "Rasm hajmi 10MB dan katta.",
          "Rasm hajmini tekshiring.");
    }
    if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(image.getContentType())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_FILE",
          "Rasm formati noto'g'ri.",
          "Rasm formatini tekshiring.");
    }
  }

  private String uploadImage(UUID schoolId, UUID examId, UUID studentId, MultipartFile image) {
    // ClamAV virus scanning (academix_tz.md §5.2) is not wired into this codebase yet — known
    // gap, flagged rather than silently skipped, same as StudentSubmissionService.
    String extension = "image/png".equals(image.getContentType()) ? "png" : "jpg";
    String key =
        "exams/%s/%s/%s/%s.%s".formatted(schoolId, examId, studentId, UUID.randomUUID(), extension);
    try {
      fileStorageService.upload(key, image.getBytes(), image.getContentType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return key;
  }

  private ExamEntity requireOwnedExam(UUID schoolId, UUID teacherId, UUID examId) {
    ExamEntity exam =
        examRepository
            .findByIdAndSchoolId(examId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_EXAM_NOT_FOUND",
                        "Imtihon topilmadi.",
                        "ID ni tekshiring."));
    if (!exam.getTeacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat imtihonni yaratgan o'qituvchi ko'rishi mumkin.");
    }
    return exam;
  }

  private ExamSubmissionEntity requireSubmission(UUID examId, UUID submissionId) {
    return submissionRepository
        .findByIdAndExamId(submissionId, examId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_EXAM_NOT_FOUND",
                    "Topshiriq topilmadi.",
                    "ID ni tekshiring."));
  }
}
