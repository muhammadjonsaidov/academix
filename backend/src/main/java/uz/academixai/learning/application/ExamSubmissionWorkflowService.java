package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Exam;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamGrade;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.ExamSubmissionWorkflow;
import uz.academixai.learning.application.port.out.ExamGradeStore;
import uz.academixai.learning.application.port.out.ExamImageStorage;
import uz.academixai.learning.application.port.out.ExamStore;
import uz.academixai.learning.application.port.out.ExamSubmissionAcceptor;
import uz.academixai.learning.application.port.out.ExamSubmissionAssessmentLookup;
import uz.academixai.learning.application.port.out.ExamSubmissionStore;
import uz.academixai.learning.application.port.out.FileSafetyScanner;
import uz.academixai.learning.application.port.out.StudentNameLookup;
import uz.academixai.learning.application.port.out.UploadQuota;

/** Learning-owned workflow for receiving and reviewing exam papers. */
@Service
public class ExamSubmissionWorkflowService implements ExamSubmissionWorkflow {

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
      List.of("image/jpeg", "image/png");

  private final ExamStore exams;
  private final ExamSubmissionStore submissions;
  private final ExamSubmissionAssessmentLookup assessments;
  private final ExamGradeStore grades;
  private final StudentNameLookup names;
  private final ExamImageStorage images;
  private final ExamSubmissionAcceptor acceptor;
  private final UploadQuota quota;
  private final FileSafetyScanner fileSafety;

  public ExamSubmissionWorkflowService(
      ExamStore exams,
      ExamSubmissionStore submissions,
      ExamSubmissionAssessmentLookup assessments,
      ExamGradeStore grades,
      StudentNameLookup names,
      ExamImageStorage images,
      ExamSubmissionAcceptor acceptor,
      UploadQuota quota,
      FileSafetyScanner fileSafety) {
    this.exams = exams;
    this.submissions = submissions;
    this.assessments = assessments;
    this.grades = grades;
    this.names = names;
    this.images = images;
    this.acceptor = acceptor;
    this.quota = quota;
    this.fileSafety = fileSafety;
  }

  @Override
  public int bulkUpload(
      UUID schoolId, UUID teacherId, UUID examId, List<Image> imageBatch, List<UUID> studentIds) {
    requireOwnedExam(schoolId, teacherId, examId);
    quota.enforce(teacherId);
    if (imageBatch.size() != studentIds.size()) {
      throw invalidFile(
          "images va studentIds soni mos kelmadi.", "Har bir rasm uchun bitta studentId yuboring.");
    }
    for (int index = 0; index < imageBatch.size(); index++) {
      Image image = imageBatch.get(index);
      validateImage(image);
      UUID studentId = studentIds.get(index);
      String imageUrl = imageKey(schoolId, examId, studentId, image.contentType());
      fileSafety.scan(image.content(), image.originalFilename());
      images.store(imageUrl, image.content(), image.contentType());
      acceptor.accept(
          new ExamSubmission(
              UUID.randomUUID(),
              schoolId,
              examId,
              studentId,
              imageUrl,
              SubmissionStatus.SUBMITTED,
              false,
              LocalDateTime.now()));
    }
    return imageBatch.size();
  }

  @Override
  public List<SubmissionWithFeedback> list(UUID schoolId, UUID teacherId, UUID examId) {
    requireOwnedExam(schoolId, teacherId, examId);
    return submissions.findByExamId(examId).stream().map(this::withFeedback).toList();
  }

  @Override
  public ExamGrade grade(
      UUID schoolId,
      UUID teacherId,
      UUID examId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment) {
    requireOwnedExam(schoolId, teacherId, examId);
    ExamSubmission submission = requireSubmission(examId, submissionId);
    ExamGrade saved =
        grades.save(
            new ExamGrade(
                grades
                    .findBySubmissionId(submissionId)
                    .map(ExamGrade::id)
                    .orElseGet(UUID::randomUUID),
                submissionId,
                teacherId,
                score,
                fivePointGrade,
                teacherComment,
                LocalDateTime.now()));
    submissions.save(
        new ExamSubmission(
            submission.id(),
            submission.schoolId(),
            submission.examId(),
            submission.studentId(),
            submission.imageUrl(),
            SubmissionStatus.GRADED,
            submission.flaggedForReview(),
            submission.uploadedAt()));
    return saved;
  }

  @Override
  public int approveAll(UUID schoolId, UUID teacherId, UUID examId) {
    requireOwnedExam(schoolId, teacherId, examId);
    List<ExamSubmission> approvable =
        submissions.findByExamId(examId).stream()
            .filter(submission -> !submission.flaggedForReview())
            .filter(submission -> submission.status() != SubmissionStatus.GRADED)
            .toList();
    for (ExamSubmission submission : approvable) {
      ExamAIFeedback feedback = assessments.feedback(submission.id()).orElse(null);
      float aiScore = feedback == null ? 0f : feedback.aiScorePercent();
      grade(
          schoolId,
          teacherId,
          examId,
          submission.id(),
          Math.round(aiScore),
          scoreToFivePoint(aiScore),
          "AI baholovi avtomatik tasdiqlandi.");
    }
    return approvable.size();
  }

  private SubmissionWithFeedback withFeedback(ExamSubmission submission) {
    return new SubmissionWithFeedback(
        submission,
        names.fullName(submission.studentId()),
        assessments.feedback(submission.id()).orElse(null),
        grades.findBySubmissionId(submission.id()).orElse(null));
  }

  private Exam requireOwnedExam(UUID schoolId, UUID teacherId, UUID examId) {
    Exam exam =
        exams
            .findByIdAndSchoolId(examId, schoolId)
            .orElseThrow(ExamSubmissionWorkflowService::examNotFound);
    if (!exam.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat imtihonni yaratgan o'qituvchi ko'rishi mumkin.");
    }
    return exam;
  }

  private ExamSubmission requireSubmission(UUID examId, UUID submissionId) {
    return submissions
        .findByIdAndExamId(submissionId, examId)
        .orElseThrow(ExamSubmissionWorkflowService::examNotFound);
  }

  private void validateImage(Image image) {
    if (image == null || image.content() == null || image.content().length == 0) {
      throw invalidFile("Rasm fayli talab qilinadi.", "Rasm yuklang.");
    }
    if (image.content().length > MAX_IMAGE_BYTES) {
      throw invalidFile("Rasm hajmi 10MB dan katta.", "Rasm hajmini tekshiring.");
    }
    if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(image.contentType())) {
      throw invalidFile("Rasm formati noto'g'ri.", "Rasm formatini tekshiring.");
    }
  }

  private static String imageKey(UUID schoolId, UUID examId, UUID studentId, String contentType) {
    String extension = "image/png".equals(contentType) ? "png" : "jpg";
    return "exams/%s/%s/%s/%s.%s"
        .formatted(schoolId, examId, studentId, UUID.randomUUID(), extension);
  }

  private static int scoreToFivePoint(float score) {
    if (score >= 87) return 5;
    if (score >= 70) return 4;
    if (score >= 50) return 3;
    return 2;
  }

  private static ApiException invalidFile(String message, String detail) {
    return new ApiException(HttpStatus.BAD_REQUEST, "ERR_INVALID_FILE", message, detail);
  }

  private static ApiException examNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_EXAM_NOT_FOUND", "Imtihon topilmadi.", "ID ni tekshiring.");
  }
}
