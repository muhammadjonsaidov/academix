package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.learning.application.port.in.HomeworkManagement;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionQuery;
import uz.academixai.learning.application.port.out.LearningNotifier;
import uz.academixai.school.application.port.in.TeacherAccess;
import uz.academixai.shared.error.ApiException;

/** Teacher homework lifecycle use cases owned by the Learning context. */
@Service
public class HomeworkManagementService implements HomeworkManagement {

  private final HomeworkAssignmentStore assignments;
  private final HomeworkSubmissionQuery submissions;
  private final TeacherAccess teacherAccess;
  private final LearningNotifier notifier;

  public HomeworkManagementService(
      HomeworkAssignmentStore assignments,
      HomeworkSubmissionQuery submissions,
      TeacherAccess teacherAccess,
      LearningNotifier notifier) {
    this.assignments = assignments;
    this.submissions = submissions;
    this.teacherAccess = teacherAccess;
    this.notifier = notifier;
  }

  @Override
  public HomeworkAssignment create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      String description,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore) {
    teacherAccess.requireAssignedToClassAndSubject(schoolId, teacherId, classId, subjectId);

    HomeworkAssignment saved =
        assignments.save(
            new HomeworkAssignment(
                UUID.randomUUID(),
                schoolId,
                classId,
                subjectId,
                teacherId,
                title,
                description,
                LocalDateTime.now(),
                deadlineAt,
                maxScore,
                true,
                syllabusReference));
    notifier.homeworkAssigned(schoolId, classId, saved.id(), title, deadlineAt);
    return saved;
  }

  @Override
  public List<HomeworkAssignment> list(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    return assignments.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
        .filter(assignment -> classId == null || assignment.classId().equals(classId))
        .filter(assignment -> subjectId == null || assignment.subjectId().equals(subjectId))
        .toList();
  }

  @Override
  public HomeworkAssignment get(UUID schoolId, UUID assignmentId) {
    return requireOwned(schoolId, assignmentId);
  }

  @Override
  public HomeworkAssignment update(
      UUID schoolId,
      UUID teacherId,
      UUID assignmentId,
      String title,
      String description,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore) {
    HomeworkAssignment existing = requireOwnedByTeacher(schoolId, teacherId, assignmentId);
    return assignments.save(
        new HomeworkAssignment(
            existing.id(),
            schoolId,
            existing.classId(),
            existing.subjectId(),
            teacherId,
            title,
            description,
            existing.assignedAt(),
            deadlineAt,
            maxScore,
            existing.isActive(),
            syllabusReference));
  }

  @Override
  public void delete(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment = requireOwnedByTeacher(schoolId, teacherId, assignmentId);
    long submissionCount = submissions.countByAssignmentId(assignmentId);
    if (submissionCount > 0) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_HW_HAS_SUBMISSIONS",
          "Bu vazifaga %d ta o'quvchi javob yuborgan — o'chirish mumkin emas."
              .formatted(submissionCount),
          "O'quvchilar javoblarini o'chirmasdan bu vazifani bekor qilib bo'lmaydi.");
    }
    assignments.delete(assignment);
  }

  private HomeworkAssignment requireOwned(UUID schoolId, UUID assignmentId) {
    return assignments
        .findByIdAndSchoolId(assignmentId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_HW_NOT_FOUND",
                    "Uy vazifasi topilmadi.",
                    "ID ni tekshiring yoki sahifani yangilang."));
  }

  private HomeworkAssignment requireOwnedByTeacher(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment = requireOwned(schoolId, assignmentId);
    if (!assignment.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni o'zgartirishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi tahrirlashi mumkin.");
    }
    return assignment;
  }
}
