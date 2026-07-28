package uz.academixai.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.infrastructure.antivirus.ClamAvScanner;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskEntity;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.queue.HomeworkSubmissionQueueProducer;
import uz.academixai.infrastructure.ratelimit.UploadRateLimiter;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.4 "Topshirish" — student submits homework (text and/or a handwriting photo),
 * which enqueues the async OCR+grading pipeline (task 22).
 */
@Service
public class StudentSubmissionService {

  // academix_tz.md §5.2 — 10MB max, JPG/PNG/PDF/DOCX allowed generally; for a homework photo
  // specifically only image formats make sense (PDF/DOCX are for syllabus uploads elsewhere).
  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
      List.of("image/jpeg", "image/png");

  private final HomeworkAssignmentRepository assignmentRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SubjectRepository subjectRepository;
  private final AIFeedbackRepository aiFeedbackRepository;
  private final GradeRepository gradeRepository;
  private final FileStorageService fileStorageService;
  private final HomeworkSubmissionQueueProducer queueProducer;
  private final StudentUniqueTaskRepository uniqueTaskRepository;
  private final UploadRateLimiter uploadRateLimiter;
  private final ClamAvScanner clamAvScanner;

  public StudentSubmissionService(
      HomeworkAssignmentRepository assignmentRepository,
      HomeworkSubmissionRepository submissionRepository,
      StudentProfileRepository studentProfileRepository,
      SubjectRepository subjectRepository,
      AIFeedbackRepository aiFeedbackRepository,
      GradeRepository gradeRepository,
      FileStorageService fileStorageService,
      HomeworkSubmissionQueueProducer queueProducer,
      StudentUniqueTaskRepository uniqueTaskRepository,
      UploadRateLimiter uploadRateLimiter,
      ClamAvScanner clamAvScanner) {
    this.uploadRateLimiter = uploadRateLimiter;
    this.clamAvScanner = clamAvScanner;
    this.assignmentRepository = assignmentRepository;
    this.submissionRepository = submissionRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.subjectRepository = subjectRepository;
    this.aiFeedbackRepository = aiFeedbackRepository;
    this.gradeRepository = gradeRepository;
    this.fileStorageService = fileStorageService;
    this.queueProducer = queueProducer;
    this.uniqueTaskRepository = uniqueTaskRepository;
  }

  public record StudentHomeworkItem(
      UUID assignmentId,
      String subjectName,
      String title,
      LocalDateTime deadlineAt,
      boolean isLate,
      String submissionStatus,
      String myTaskContent) {}

  public record StudentSubmissionDetail(
      HomeworkSubmission submission, AIFeedbackEntity feedback, GradeEntity grade) {}

  public List<StudentHomeworkItem> listHomework(UUID schoolId, UUID studentId) {
    UUID classId = requireStudentClassId(schoolId, studentId);
    return assignmentRepository
        .findBySchoolIdAndClassIdOrderByDeadlineAtDesc(schoolId, classId)
        .stream()
        // UNIQUE_GENERATED assignments stay hidden until the teacher publishes via
        // POST /teacher/homework/{id}/submit (see HomeworkAssignment.tasksPublished).
        .filter(HomeworkAssignmentEntity::isTasksPublished)
        .map(a -> buildHomeworkItem(a, studentId))
        .toList();
  }

  public StudentHomeworkItem getHomeworkDetail(UUID schoolId, UUID studentId, UUID assignmentId) {
    HomeworkAssignmentEntity assignment = requireAssignment(schoolId, assignmentId);
    requireStudentInClass(schoolId, studentId, assignment.getClassId());
    requirePublished(assignment);
    return buildHomeworkItem(assignment, studentId);
  }

  public List<HomeworkSubmission> listSubmissions(UUID schoolId, UUID studentId) {
    return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .filter(s -> schoolId.equals(s.schoolId()))
        .toList();
  }

  public StudentSubmissionDetail getSubmissionDetail(
      UUID schoolId, UUID studentId, UUID submissionId) {
    HomeworkSubmissionEntity subEntity =
        submissionRepository
            .findByIdAndSchoolId(submissionId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_HW_NOT_FOUND",
                        "Topshiriq topilmadi.",
                        "ID ni tekshiring yoki sahifani yangilang."));
    if (!subEntity.getStudentId().equals(studentId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat o'zingizning topshiriqlaringizni ko'rishingiz mumkin.");
    }
    return new StudentSubmissionDetail(
        subEntity.toDomain(),
        aiFeedbackRepository.findBySubmissionId(submissionId).orElse(null),
        gradeRepository.findBySubmissionId(submissionId).orElse(null));
  }

  private StudentHomeworkItem buildHomeworkItem(
      HomeworkAssignmentEntity assignmentEntity, UUID studentId) {
    var assignment = assignmentEntity.toDomain();
    String subjectName =
        subjectRepository
            .findById(assignment.subjectId())
            .map(SubjectEntity::getName)
            .orElse("Fan");
    var existingSubmission =
        submissionRepository
            .findByAssignmentIdAndStudentId(assignment.id(), studentId)
            .map(HomeworkSubmissionEntity::toDomain);
    String submissionStatus =
        existingSubmission
            .map(s -> s.status() == SubmissionStatus.GRADED ? "GRADED" : "SUBMITTED")
            .orElse("PENDING");
    boolean isLate =
        existingSubmission
            .map(HomeworkSubmission::isLate)
            .orElseGet(() -> LocalDateTime.now().isAfter(assignment.deadlineAt()));
    String myTaskContent =
        assignment.type() == AssignmentType.UNIQUE_GENERATED
            ? uniqueTaskRepository
                .findByAssignmentIdAndStudentId(assignment.id(), studentId)
                .map(StudentUniqueTaskEntity::toDomain)
                .map(StudentUniqueTask::taskContent)
                .orElse(null)
            : null;
    return new StudentHomeworkItem(
        assignment.id(),
        subjectName,
        assignment.title(),
        assignment.deadlineAt(),
        isLate,
        submissionStatus,
        myTaskContent);
  }

  private void requirePublished(HomeworkAssignmentEntity assignment) {
    if (!assignment.isTasksPublished()) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "ERR_HW_NOT_FOUND",
          "Uy vazifasi topilmadi.",
          "ID ni tekshiring yoki sahifani yangilang.");
    }
  }

  private UUID requireStudentClassId(UUID schoolId, UUID studentId) {
    return studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::toDomain)
        .filter(p -> schoolId.equals(p.schoolId()))
        .map(p -> p.classId())
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ERR_ACCESS_DENIED",
                    "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                    "O'quvchi profili topilmadi."));
  }

  public HomeworkSubmission submit(
      UUID schoolId,
      UUID studentId,
      UUID assignmentId,
      SubmissionType type,
      String textContent,
      MultipartFile image) {
    HomeworkAssignmentEntity assignment = requireAssignment(schoolId, assignmentId);
    requireStudentInClass(schoolId, studentId, assignment.getClassId());
    requirePublished(assignment);
    requireNotAlreadySubmitted(assignmentId, studentId);
    requireValidPayload(type, textContent, image);

    UUID studentTaskId =
        assignment.toDomain().type() == AssignmentType.UNIQUE_GENERATED
            ? uniqueTaskRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(StudentUniqueTaskEntity::getId)
                .orElse(null)
            : null;

    boolean hasFile = image != null && !image.isEmpty();
    if (hasFile) {
      // Charged before the upload, and only when a file is really present — a TEXT-only
      // submission moves no bytes and shouldn't consume the allowance (academix_tz.md §5.3).
      uploadRateLimiter.enforce(studentId);
    }

    String imageUrl = hasFile ? uploadImage(schoolId, assignmentId, studentId, image) : null;

    HomeworkSubmission submission =
        new HomeworkSubmission(
            UUID.randomUUID(),
            schoolId,
            assignmentId,
            studentTaskId,
            studentId,
            type,
            textContent,
            imageUrl,
            SubmissionStatus.SUBMITTED,
            LocalDateTime.now().isAfter(assignment.toDomain().deadlineAt()),
            LocalDateTime.now(),
            0);
    HomeworkSubmission saved =
        submissionRepository.save(HomeworkSubmissionEntity.fromDomain(submission)).toDomain();

    queueProducer.publish(saved.id(), schoolId);
    return saved;
  }

  private HomeworkAssignmentEntity requireAssignment(UUID schoolId, UUID assignmentId) {
    return assignmentRepository
        .findByIdAndSchoolId(assignmentId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_HW_NOT_FOUND",
                    "Uy vazifasi topilmadi.",
                    "ID ni tekshiring yoki sahifani yangilang."));
  }

  private void requireStudentInClass(UUID schoolId, UUID studentId, UUID classId) {
    var profile =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::toDomain)
            .filter(p -> schoolId.equals(p.schoolId()))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.FORBIDDEN,
                        "ERR_ACCESS_DENIED",
                        "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                        "O'quvchi profili topilmadi."));
    if (!classId.equals(profile.classId())) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu vazifa sizning sinfingizga tegishli emas.",
          "O'qituvchi bilan bog'laning.");
    }
  }

  private void requireNotAlreadySubmitted(UUID assignmentId, UUID studentId) {
    if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_ALREADY_SUBMITTED",
          "Siz bu vazifani allaqachon topshirgansiz.",
          "Qayta topshirish uchun o'qituvchiga murojaat qiling.");
    }
  }

  private void requireValidPayload(SubmissionType type, String textContent, MultipartFile image) {
    boolean hasText = textContent != null && !textContent.isBlank();
    boolean hasImage = image != null && !image.isEmpty();

    switch (type) {
      case TEXT -> {
        if (!hasText) {
          throw invalidPayload("textContent talab qilinadi.");
        }
      }
      case IMAGE -> {
        if (!hasImage) {
          throw invalidPayload("Rasm fayli talab qilinadi.");
        }
      }
      case MIXED -> {
        if (!hasText || !hasImage) {
          throw invalidPayload("MIXED turi uchun ham matn, ham rasm talab qilinadi.");
        }
      }
    }

    if (hasImage) {
      if (image.getSize() > MAX_IMAGE_BYTES) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "ERR_INVALID_FILE",
            "Rasm hajmi 10MB dan katta.",
            "Rasm hajmini tekshirishni so'rash.");
      }
      if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(image.getContentType())) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "ERR_INVALID_FILE",
            "Rasm formati noto'g'ri.",
            "Rasm formatini tekshirishni so'rash.");
      }
    }
  }

  private String uploadImage(
      UUID schoolId, UUID assignmentId, UUID studentId, MultipartFile image) {
    // academix_tz.md §5.2 — scan before a single byte reaches SeaweedFS, so an infected file is
    // never persisted even briefly. No-op unless academix.clamav.enabled=true (see ClamAvScanner).
    clamAvScanner.scan(image);

    String extension = image.getContentType().equals("image/png") ? "png" : "jpg";
    String key =
        "homeworks/%s/%s/%s/%s.%s"
            .formatted(schoolId, assignmentId, studentId, UUID.randomUUID(), extension);
    try {
      fileStorageService.upload(key, image.getBytes(), image.getContentType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return key;
  }

  private static ApiException invalidPayload(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST, "ERR_INVALID_FILE", message, "So'rov tarkibini tekshiring.");
  }
}
