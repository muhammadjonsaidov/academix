package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.UniqueTaskReview;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.StudentNameLookup;
import uz.academixai.learning.application.port.out.StudentUniqueTaskStore;

/** Teacher review, approval and publish use cases for generated Learning tasks. */
@Service
public class UniqueTaskReviewService implements UniqueTaskReview {

  private final HomeworkAssignmentStore assignments;
  private final StudentUniqueTaskStore tasks;
  private final StudentNameLookup studentNames;

  public UniqueTaskReviewService(
      HomeworkAssignmentStore assignments,
      StudentUniqueTaskStore tasks,
      StudentNameLookup studentNames) {
    this.assignments = assignments;
    this.tasks = tasks;
    this.studentNames = studentNames;
  }

  @Override
  public List<TaskWithStudent> list(UUID schoolId, UUID teacherId, UUID assignmentId) {
    requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    return tasks.findByAssignmentId(assignmentId).stream()
        .map(task -> new TaskWithStudent(task, studentNames.fullName(task.studentId())))
        .toList();
  }

  @Override
  public StudentUniqueTask approve(UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId) {
    requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    StudentUniqueTask task = requireTask(assignmentId, taskId);
    return tasks.save(withApproval(task, true, LocalDateTime.now()));
  }

  @Override
  public StudentUniqueTask editContent(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId, String taskContent) {
    requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    StudentUniqueTask task = requireTask(assignmentId, taskId);
    return tasks.save(
        new StudentUniqueTask(
            task.id(),
            task.assignmentId(),
            task.studentId(),
            taskContent,
            task.teacherApproved(),
            task.flaggedForReview(),
            task.fallbackToStandard(),
            task.generatedAt(),
            task.approvedAt()));
  }

  @Override
  public int approveAll(UUID schoolId, UUID teacherId, UUID assignmentId) {
    requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    List<StudentUniqueTask> unflagged = tasks.findUnflaggedByAssignmentId(assignmentId);
    for (StudentUniqueTask task : unflagged) {
      tasks.save(withApproval(task, true, LocalDateTime.now()));
    }
    return unflagged.size();
  }

  @Override
  public void submit(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment = requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    List<StudentUniqueTask> generated = tasks.findByAssignmentId(assignmentId);
    if (generated.isEmpty() || generated.stream().anyMatch(task -> !task.teacherApproved())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_UNIQUE_TASKS_NOT_APPROVED",
          "Barcha unique topshiriqlar tasdiqlanmagan.",
          "Yuborishdan oldin barcha topshiriqlarni ko'rib chiqing va tasdiqlang.");
    }
    assignments.save(
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
            true));
  }

  private HomeworkAssignment requireOwnedUniqueAssignment(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_HW_NOT_FOUND",
                        "Uy vazifasi topilmadi.",
                        "ID ni tekshiring."));
    if (!assignment.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu vazifa sizga tegishli emas.",
          "Faqat o'zingiz yaratgan vazifalar bilan ishlang.");
    }
    if (assignment.type() != AssignmentType.UNIQUE_GENERATED) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_ASSIGNMENT_TYPE",
          "Bu vazifa UNIQUE_GENERATED turida emas.",
          "Faqat unique-generatsiya turidagi vazifalar uchun ishlaydi.");
    }
    return assignment;
  }

  private StudentUniqueTask requireTask(UUID assignmentId, UUID taskId) {
    return tasks
        .findByIdAndAssignmentId(taskId, assignmentId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_UNIQUE_TASK_NOT_FOUND",
                    "Unique topshiriq topilmadi.",
                    "ID ni tekshiring."));
  }

  private static StudentUniqueTask withApproval(
      StudentUniqueTask task, boolean approved, LocalDateTime approvedAt) {
    return new StudentUniqueTask(
        task.id(),
        task.assignmentId(),
        task.studentId(),
        task.taskContent(),
        approved,
        task.flaggedForReview(),
        task.fallbackToStandard(),
        task.generatedAt(),
        approvedAt);
  }
}
