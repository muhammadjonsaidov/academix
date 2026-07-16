package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.interfaces.web.ApiException;

/** academix_tz.md §2.3 "Uy vazifasi yaratish" — teacher CRUD over homework_assignments. */
@Service
public class HomeworkService {

  private final HomeworkAssignmentRepository assignmentRepository;
  private final TeacherContextService teacherContextService;
  private final UniqueTaskGenerationService uniqueTaskGenerationService;

  public HomeworkService(
      HomeworkAssignmentRepository assignmentRepository,
      TeacherContextService teacherContextService,
      UniqueTaskGenerationService uniqueTaskGenerationService) {
    this.assignmentRepository = assignmentRepository;
    this.teacherContextService = teacherContextService;
    this.uniqueTaskGenerationService = uniqueTaskGenerationService;
  }

  public HomeworkAssignment create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      String description,
      AssignmentType type,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore) {
    teacherContextService.requireAssignedToClassAndSubject(schoolId, teacherId, classId, subjectId);

    HomeworkAssignment assignment =
        new HomeworkAssignment(
            UUID.randomUUID(),
            schoolId,
            classId,
            subjectId,
            teacherId,
            title,
            description,
            type,
            LocalDateTime.now(),
            deadlineAt,
            maxScore,
            true,
            syllabusReference,
            null,
            // UNIQUE_GENERATED assignments stay hidden from students until the teacher reviews
            // and publishes generated tasks via POST .../submit (academix_tz.md §2.3) — STANDARD
            // has no review step, visible immediately.
            type != AssignmentType.UNIQUE_GENERATED);
    HomeworkAssignment saved =
        assignmentRepository.save(HomeworkAssignmentEntity.fromDomain(assignment)).toDomain();
    if (type == AssignmentType.UNIQUE_GENERATED) {
      uniqueTaskGenerationService.generateUniqueTasks(schoolId, teacherId, saved.id());
    }
    return saved;
  }

  // classId/subjectId filters are documented (academix_tz.md §2.3); the query also lists a
  // "status" param with no enumerated values or definition anywhere in the spec docs (known gap
  // — homework_assignments has no status field, only isActive) — not implemented until that's
  // clarified, rather than guessing wrong semantics.
  public List<HomeworkAssignment> list(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    List<HomeworkAssignmentEntity> all =
        assignmentRepository.findBySchoolIdAndTeacherIdOrderByDeadlineAtDesc(schoolId, teacherId);
    return all.stream()
        .filter(a -> classId == null || a.getClassId().equals(classId))
        .filter(a -> subjectId == null || a.getSubjectId().equals(subjectId))
        .map(HomeworkAssignmentEntity::toDomain)
        .toList();
  }

  public HomeworkAssignment get(UUID schoolId, UUID assignmentId) {
    return requireOwned(schoolId, assignmentId).toDomain();
  }

  public HomeworkAssignment update(
      UUID schoolId,
      UUID teacherId,
      UUID assignmentId,
      String title,
      String description,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore) {
    HomeworkAssignment existing =
        requireOwnedByTeacher(schoolId, teacherId, assignmentId).toDomain();
    HomeworkAssignment updated =
        new HomeworkAssignment(
            existing.id(),
            schoolId,
            existing.classId(),
            existing.subjectId(),
            teacherId,
            title,
            description,
            existing.type(),
            existing.assignedAt(),
            deadlineAt,
            maxScore,
            existing.isActive(),
            syllabusReference,
            existing.aiGenerationPrompt(),
            existing.tasksPublished());
    return assignmentRepository.save(HomeworkAssignmentEntity.fromDomain(updated)).toDomain();
  }

  public void delete(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity entity = requireOwnedByTeacher(schoolId, teacherId, assignmentId);
    assignmentRepository.delete(entity);
  }

  private HomeworkAssignmentEntity requireOwned(UUID schoolId, UUID assignmentId) {
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

  private HomeworkAssignmentEntity requireOwnedByTeacher(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity entity = requireOwned(schoolId, assignmentId);
    if (!entity.getTeacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni o'zgartirishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi tahrirlashi mumkin.");
    }
    return entity;
  }
}
