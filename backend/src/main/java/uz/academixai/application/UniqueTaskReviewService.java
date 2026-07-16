package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskEntity;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Unique vazifalarni ko'rish va tasdiqlash" — teacher review/approve flow over
 * {@link StudentUniqueTask}, and the final publish step that makes them visible to students.
 */
@Service
public class UniqueTaskReviewService {

  private final HomeworkAssignmentRepository assignmentRepository;
  private final StudentUniqueTaskRepository uniqueTaskRepository;
  private final UserRepository userRepository;

  public UniqueTaskReviewService(
      HomeworkAssignmentRepository assignmentRepository,
      StudentUniqueTaskRepository uniqueTaskRepository,
      UserRepository userRepository) {
    this.assignmentRepository = assignmentRepository;
    this.uniqueTaskRepository = uniqueTaskRepository;
    this.userRepository = userRepository;
  }

  public record UniqueTaskWithStudent(StudentUniqueTask task, String studentName) {}

  public List<UniqueTaskWithStudent> list(UUID schoolId, UUID teacherId, UUID assignmentId) {
    requireOwnedAssignment(schoolId, teacherId, assignmentId);
    return uniqueTaskRepository.findByAssignmentId(assignmentId).stream()
        .map(StudentUniqueTaskEntity::toDomain)
        .map(t -> new UniqueTaskWithStudent(t, studentName(t.studentId())))
        .toList();
  }

  public StudentUniqueTask approve(UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId) {
    requireOwnedAssignment(schoolId, teacherId, assignmentId);
    StudentUniqueTaskEntity entity = requireTask(assignmentId, taskId);
    StudentUniqueTask existing = entity.toDomain();
    StudentUniqueTask approved =
        new StudentUniqueTask(
            existing.id(),
            existing.assignmentId(),
            existing.studentId(),
            existing.taskContent(),
            true,
            existing.flaggedForReview(),
            existing.fallbackToStandard(),
            existing.generatedAt(),
            LocalDateTime.now());
    return uniqueTaskRepository.save(StudentUniqueTaskEntity.fromDomain(approved)).toDomain();
  }

  public StudentUniqueTask editContent(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId, String taskContent) {
    requireOwnedAssignment(schoolId, teacherId, assignmentId);
    StudentUniqueTaskEntity entity = requireTask(assignmentId, taskId);
    StudentUniqueTask existing = entity.toDomain();
    StudentUniqueTask edited =
        new StudentUniqueTask(
            existing.id(),
            existing.assignmentId(),
            existing.studentId(),
            taskContent,
            existing.teacherApproved(),
            existing.flaggedForReview(),
            existing.fallbackToStandard(),
            existing.generatedAt(),
            existing.approvedAt());
    return uniqueTaskRepository.save(StudentUniqueTaskEntity.fromDomain(edited)).toDomain();
  }

  /** Bulk-approves only non-flagged tasks — flagged ones require individual review per spec. */
  public int approveAll(UUID schoolId, UUID teacherId, UUID assignmentId) {
    requireOwnedAssignment(schoolId, teacherId, assignmentId);
    var toApprove = uniqueTaskRepository.findByAssignmentIdAndFlaggedForReviewFalse(assignmentId);
    for (StudentUniqueTaskEntity entity : toApprove) {
      StudentUniqueTask existing = entity.toDomain();
      StudentUniqueTask approved =
          new StudentUniqueTask(
              existing.id(),
              existing.assignmentId(),
              existing.studentId(),
              existing.taskContent(),
              true,
              existing.flaggedForReview(),
              existing.fallbackToStandard(),
              existing.generatedAt(),
              LocalDateTime.now());
      uniqueTaskRepository.save(StudentUniqueTaskEntity.fromDomain(approved));
    }
    return toApprove.size();
  }

  /** academix_tz.md §2.3 — publishes to students only once every generated task is approved. */
  public void submit(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity assignmentEntity =
        requireOwnedAssignment(schoolId, teacherId, assignmentId);
    var tasks = uniqueTaskRepository.findByAssignmentId(assignmentId);
    boolean allApproved =
        !tasks.isEmpty() && tasks.stream().allMatch(e -> e.toDomain().teacherApproved());
    if (!allApproved) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_UNIQUE_TASKS_NOT_APPROVED",
          "Barcha unique topshiriqlar tasdiqlanmagan.",
          "Yuborishdan oldin barcha topshiriqlarni ko'rib chiqing va tasdiqlang.");
    }
    var assignment = assignmentEntity.toDomain();
    HomeworkAssignment published =
        new HomeworkAssignment(
            assignment.id(),
            assignment.schoolId(),
            assignment.classId(),
            assignment.subjectId(),
            assignment.teacherId(),
            assignment.title(),
            assignment.description(),
            assignment.type(),
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.maxScore(),
            assignment.isActive(),
            assignment.syllabusReference(),
            assignment.aiGenerationPrompt(),
            true);
    assignmentRepository.save(HomeworkAssignmentEntity.fromDomain(published));
  }

  private HomeworkAssignmentEntity requireOwnedAssignment(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity entity =
        assignmentRepository
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_HW_NOT_FOUND",
                        "Uy vazifasi topilmadi.",
                        "ID ni tekshiring."));
    if (!entity.getTeacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu vazifa sizga tegishli emas.",
          "Faqat o'zingiz yaratgan vazifalar bilan ishlang.");
    }
    if (entity.toDomain().type() != AssignmentType.UNIQUE_GENERATED) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_ASSIGNMENT_TYPE",
          "Bu vazifa UNIQUE_GENERATED turida emas.",
          "Faqat unique-generatsiya turidagi vazifalar uchun ishlaydi.");
    }
    return entity;
  }

  private StudentUniqueTaskEntity requireTask(UUID assignmentId, UUID taskId) {
    return uniqueTaskRepository
        .findByIdAndAssignmentId(taskId, assignmentId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_UNIQUE_TASK_NOT_FOUND",
                    "Unique topshiriq topilmadi.",
                    "ID ni tekshiring."));
  }

  private String studentName(UUID studentId) {
    return userRepository
        .findById(studentId)
        .map(u -> u.getFirstName() + " " + u.getLastName())
        .orElse("Noma'lum");
  }
}
