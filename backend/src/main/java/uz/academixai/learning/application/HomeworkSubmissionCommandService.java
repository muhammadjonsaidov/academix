package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.learning.application.port.in.HomeworkSubmissionCommand;
import uz.academixai.learning.application.port.out.FileSafetyScanner;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkImageStorage;
import uz.academixai.learning.application.port.out.HomeworkSubmissionPublisher;
import uz.academixai.learning.application.port.out.HomeworkSubmissionStore;
import uz.academixai.learning.application.port.out.StudentClassLookup;
import uz.academixai.learning.application.port.out.StudentUniqueTaskStore;
import uz.academixai.learning.application.port.out.UploadQuota;
import uz.academixai.shared.error.ApiException;

/** Accepts a homework submission, validates its image and records a durable processing event. */
@Service
public class HomeworkSubmissionCommandService implements HomeworkSubmissionCommand {

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
      List.of("image/jpeg", "image/png");

  private final HomeworkAssignmentStore assignments;
  private final HomeworkSubmissionStore submissions;
  private final StudentClassLookup studentClasses;
  private final StudentUniqueTaskStore uniqueTasks;
  private final UploadQuota uploadQuota;
  private final FileSafetyScanner safetyScanner;
  private final HomeworkImageStorage images;
  private final HomeworkSubmissionPublisher publisher;

  public HomeworkSubmissionCommandService(
      HomeworkAssignmentStore assignments,
      HomeworkSubmissionStore submissions,
      StudentClassLookup studentClasses,
      StudentUniqueTaskStore uniqueTasks,
      UploadQuota uploadQuota,
      FileSafetyScanner safetyScanner,
      HomeworkImageStorage images,
      HomeworkSubmissionPublisher publisher) {
    this.assignments = assignments;
    this.submissions = submissions;
    this.studentClasses = studentClasses;
    this.uniqueTasks = uniqueTasks;
    this.uploadQuota = uploadQuota;
    this.safetyScanner = safetyScanner;
    this.images = images;
    this.publisher = publisher;
  }

  @Override
  public HomeworkSubmission submit(
      UUID schoolId,
      UUID studentId,
      UUID assignmentId,
      SubmissionType type,
      String textContent,
      Image image) {
    HomeworkAssignment assignment = requirePublishedAssignment(schoolId, assignmentId);
    requireStudentInClass(schoolId, studentId, assignment.classId());
    if (submissions.existsByAssignmentIdAndStudentId(assignmentId, studentId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_ALREADY_SUBMITTED",
          "Siz bu vazifani allaqachon topshirgansiz.",
          "Qayta topshirish uchun o'qituvchiga murojaat qiling.");
    }
    validatePayload(type, textContent, image);

    String imageUrl = image == null ? null : storeImage(schoolId, assignmentId, studentId, image);
    UUID studentTaskId =
        assignment.type() == AssignmentType.UNIQUE_GENERATED
            ? uniqueTasks
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(task -> task.id())
                .orElse(null)
            : null;
    HomeworkSubmission saved =
        submissions.save(
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
                LocalDateTime.now().isAfter(assignment.deadlineAt()),
                LocalDateTime.now(),
                0));
    publisher.publish(saved.id(), schoolId);
    return saved;
  }

  private HomeworkAssignment requirePublishedAssignment(UUID schoolId, UUID assignmentId) {
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(HomeworkSubmissionCommandService::homeworkNotFound);
    if (!assignment.tasksPublished()) {
      throw homeworkNotFound();
    }
    return assignment;
  }

  private void requireStudentInClass(UUID schoolId, UUID studentId, UUID classId) {
    UUID studentClass =
        studentClasses
            .classId(schoolId, studentId)
            .orElseThrow(HomeworkSubmissionCommandService::studentAccessDenied);
    if (!classId.equals(studentClass)) {
      throw studentAccessDenied();
    }
  }

  private void validatePayload(SubmissionType type, String textContent, Image image) {
    boolean hasText = textContent != null && !textContent.isBlank();
    boolean hasImage = image != null && image.content() != null && image.content().length > 0;
    if ((type == SubmissionType.TEXT && !hasText)
        || (type == SubmissionType.IMAGE && !hasImage)
        || (type == SubmissionType.MIXED && (!hasText || !hasImage))) {
      throw invalidPayload("So'rov turi uchun talab qilinadigan matn yoki rasm yo'q.");
    }
    if (hasImage) {
      if (image.content().length > MAX_IMAGE_BYTES) {
        throw invalidPayload("Rasm hajmi 10MB dan katta.");
      }
      if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(image.contentType())) {
        throw invalidPayload("Rasm formati noto'g'ri.");
      }
    }
  }

  private String storeImage(UUID schoolId, UUID assignmentId, UUID studentId, Image image) {
    uploadQuota.enforce(studentId);
    safetyScanner.scan(image.content(), "homework-upload");
    String extension = "image/png".equals(image.contentType()) ? "png" : "jpg";
    String key =
        "homeworks/%s/%s/%s/%s.%s"
            .formatted(schoolId, assignmentId, studentId, UUID.randomUUID(), extension);
    images.store(key, image.content(), image.contentType());
    return key;
  }

  private static ApiException homeworkNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Uy vazifasi topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }

  private static ApiException studentAccessDenied() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        "ERR_ACCESS_DENIED",
        "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
        "O'quvchi profili topilmadi yoki vazifa sizning sinfingizga tegishli emas.");
  }

  private static ApiException invalidPayload(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST, "ERR_INVALID_FILE", message, "So'rov tarkibini tekshiring.");
  }
}
