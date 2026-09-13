package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.out.ClassStudentQuery;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.StudentUniqueTaskStore;
import uz.academixai.learning.application.port.out.UniqueTaskAi;
import uz.academixai.learning.application.port.out.UniqueTaskContentValidator;
import uz.academixai.learning.application.port.out.UniqueTaskContextLookup;
import uz.academixai.learning.application.port.out.UniqueTaskGenerator;

/** Generates one independently checked, idempotent task per student for a unique assignment. */
@Service
public class UniqueTaskGenerationService implements UniqueTaskGenerator {

  private static final Logger log = LoggerFactory.getLogger(UniqueTaskGenerationService.class);
  private static final int MAX_ATTEMPTS = 2;

  private final HomeworkAssignmentStore assignments;
  private final StudentUniqueTaskStore tasks;
  private final ClassStudentQuery classStudents;
  private final UniqueTaskContextLookup contextLookup;
  private final UniqueTaskAi ai;
  private final UniqueTaskContentValidator validator;

  public UniqueTaskGenerationService(
      HomeworkAssignmentStore assignments,
      StudentUniqueTaskStore tasks,
      ClassStudentQuery classStudents,
      UniqueTaskContextLookup contextLookup,
      UniqueTaskAi ai,
      UniqueTaskContentValidator validator) {
    this.assignments = assignments;
    this.tasks = tasks;
    this.classStudents = classStudents;
    this.contextLookup = contextLookup;
    this.ai = ai;
    this.validator = validator;
  }

  @Override
  public void generate(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment = requireOwnedUniqueAssignment(schoolId, teacherId, assignmentId);
    UniqueTaskContextLookup.Context context =
        contextLookup.find(assignment.subjectId(), assignment.classId());
    for (UUID studentId : classStudents.studentIds(schoolId, assignment.classId())) {
      if (tasks.findByAssignmentIdAndStudentId(assignmentId, studentId).isPresent()) {
        continue;
      }
      tasks.save(generateForStudent(assignmentId, studentId, assignment.description(), context));
    }
  }

  private StudentUniqueTask generateForStudent(
      UUID assignmentId,
      UUID studentId,
      String standardDescription,
      UniqueTaskContextLookup.Context context) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      String generated = tryGenerateAndVerify(standardDescription, context);
      if (generated != null) {
        return new StudentUniqueTask(
            UUID.randomUUID(),
            assignmentId,
            studentId,
            generated,
            false,
            false,
            false,
            LocalDateTime.now(),
            null);
      }
    }
    return new StudentUniqueTask(
        UUID.randomUUID(),
        assignmentId,
        studentId,
        standardDescription,
        false,
        true,
        true,
        LocalDateTime.now(),
        null);
  }

  private String tryGenerateAndVerify(
      String standardDescription, UniqueTaskContextLookup.Context context) {
    String generated;
    try {
      generated = ai.generate(context.subjectAndGrade(), standardDescription);
      if (generated == null || generated.isBlank()) {
        return null;
      }
      if (!validator.isSolvable(context.subjectType(), generated)) {
        return null;
      }
      return ai.verifySolvable(generated) ? generated : null;
    } catch (RuntimeException exception) {
      log.warn(
          "Unique-task AI generation/verification unavailable; using standard fallback", exception);
      return null;
    }
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
          "Faqat o'zingiz yaratgan vazifalar uchun unique topshiriq generatsiya qiling.");
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
}
