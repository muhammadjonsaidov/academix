package uz.academixai.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.queue.HomeworkSubmissionQueueProducer;
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
  private final FileStorageService fileStorageService;
  private final HomeworkSubmissionQueueProducer queueProducer;

  public StudentSubmissionService(
      HomeworkAssignmentRepository assignmentRepository,
      HomeworkSubmissionRepository submissionRepository,
      StudentProfileRepository studentProfileRepository,
      FileStorageService fileStorageService,
      HomeworkSubmissionQueueProducer queueProducer) {
    this.assignmentRepository = assignmentRepository;
    this.submissionRepository = submissionRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.fileStorageService = fileStorageService;
    this.queueProducer = queueProducer;
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
    requireNotAlreadySubmitted(assignmentId, studentId);
    requireValidPayload(type, textContent, image);

    String imageUrl =
        image == null || image.isEmpty()
            ? null
            : uploadImage(schoolId, assignmentId, studentId, image);

    HomeworkSubmission submission =
        new HomeworkSubmission(
            UUID.randomUUID(),
            schoolId,
            assignmentId,
            null,
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
    // ClamAV virus scanning (academix_tz.md §5.2) is not wired into this codebase yet — known
    // gap, flagged rather than silently skipped. See CLAUDE.md "Reality checks".
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
