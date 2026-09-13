package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.application.port.out.ai.AiProviderUnavailableException;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.infrastructure.ai.UniqueTaskValidator;
import uz.academixai.infrastructure.ai.UniqueTaskValidatorRegistry;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskEntity;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.9 — per-student unique homework task generation. Correctness check: an optional
 * deterministic pre-check (cheap, exact) followed by an independent, context-free Qwen verification
 * call (deliberately not a self-check in the same call, to avoid a correlated failure). Fails twice
 * → {@code fallbackToStandard=true}, {@code flaggedForReview=true}, that one student gets the
 * STANDARD assignment description instead; other students are unaffected.
 *
 * <p>Idempotent per call: students who already have a {@link StudentUniqueTask} row for this
 * assignment are skipped, so re-triggering generation (e.g. after fixing a subject's grading
 * criteria) doesn't duplicate rows or overwrite a teacher's in-progress review.
 */
@Service
public class UniqueTaskGenerationService {

  private static final Logger log = LoggerFactory.getLogger(UniqueTaskGenerationService.class);

  private static final int MAX_ATTEMPTS = 2;

  private final HomeworkAssignmentRepository assignmentRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final StudentUniqueTaskRepository uniqueTaskRepository;
  private final SubjectRepository subjectRepository;
  private final SchoolClassRepository classRepository;
  private final AiProvider aiClient;
  private final UniqueTaskValidatorRegistry validatorRegistry;

  public UniqueTaskGenerationService(
      HomeworkAssignmentRepository assignmentRepository,
      StudentProfileRepository studentProfileRepository,
      StudentUniqueTaskRepository uniqueTaskRepository,
      SubjectRepository subjectRepository,
      SchoolClassRepository classRepository,
      AiProvider aiClient,
      UniqueTaskValidatorRegistry validatorRegistry) {
    this.assignmentRepository = assignmentRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.uniqueTaskRepository = uniqueTaskRepository;
    this.subjectRepository = subjectRepository;
    this.classRepository = classRepository;
    this.aiClient = aiClient;
    this.validatorRegistry = validatorRegistry;
  }

  public List<StudentUniqueTask> generateUniqueTasks(
      UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity assignmentEntity =
        assignmentRepository
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_HW_NOT_FOUND",
                        "Uy vazifasi topilmadi.",
                        "ID ni tekshiring."));
    var assignment = assignmentEntity.toDomain();
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

    var subjectType =
        subjectRepository.findById(assignment.subjectId()).map(SubjectEntity::getType).orElse(null);
    String subjectAndGrade = subjectAndGrade(assignment.subjectId(), assignment.classId());
    Optional<UniqueTaskValidator> validator =
        subjectType == null ? Optional.empty() : validatorRegistry.forSubject(subjectType);

    List<StudentUniqueTask> results = new ArrayList<>();
    for (StudentProfileEntity student :
        studentProfileRepository.findByClassIdAndSchoolId(assignment.classId(), schoolId)) {
      UUID studentId = student.getUserId();
      var existing = uniqueTaskRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
      if (existing.isPresent()) {
        results.add(existing.get().toDomain());
        continue;
      }
      results.add(
          generateForStudent(
              assignmentId, studentId, subjectAndGrade, assignment.description(), validator));
    }
    return results;
  }

  private StudentUniqueTask generateForStudent(
      UUID assignmentId,
      UUID studentId,
      String subjectAndGrade,
      String standardDescription,
      Optional<UniqueTaskValidator> validator) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      String generated = tryGenerateAndVerify(subjectAndGrade, standardDescription, validator);
      if (generated != null) {
        StudentUniqueTask task =
            new StudentUniqueTask(
                UUID.randomUUID(),
                assignmentId,
                studentId,
                generated,
                false,
                false,
                false,
                LocalDateTime.now(),
                null);
        return uniqueTaskRepository.save(StudentUniqueTaskEntity.fromDomain(task)).toDomain();
      }
    }
    // academix_tz.md §1.9 step 3 — 2 failed attempts, fall back to the STANDARD assignment
    // content for this one student, flagged for the teacher to review.
    StudentUniqueTask fallback =
        new StudentUniqueTask(
            UUID.randomUUID(),
            assignmentId,
            studentId,
            standardDescription,
            false,
            true,
            true,
            LocalDateTime.now(),
            null);
    return uniqueTaskRepository.save(StudentUniqueTaskEntity.fromDomain(fallback)).toDomain();
  }

  /** Returns the generated task content on success, or {@code null} if this attempt failed. */
  private String tryGenerateAndVerify(
      String subjectAndGrade, String standardDescription, Optional<UniqueTaskValidator> validator) {
    String generated;
    try {
      generated = aiClient.generateUniqueTask(subjectAndGrade, standardDescription);
    } catch (AiProviderUnavailableException e) {
      // Graceful per-student fallback (correct — don't fail the whole batch), but silently
      // returning null makes "why did this student fall back to standard" undiagnosable
      // without a log line, same class of gap fixed elsewhere in the AI catch sites.
      log.warn("AI provider unique-task generation unavailable, falling back to standard", e);
      return null;
    }
    if (generated == null || generated.isBlank()) {
      return null;
    }
    if (validator.isPresent() && !validator.get().isSolvable(generated)) {
      return null;
    }
    try {
      return aiClient.verifyTaskSolvable(generated) ? generated : null;
    } catch (AiProviderUnavailableException e) {
      log.warn("AI provider task verification unavailable, falling back to standard", e);
      return null;
    }
  }

  private String subjectAndGrade(UUID subjectId, UUID classId) {
    String subjectName =
        subjectRepository.findById(subjectId).map(SubjectEntity::getName).orElse("Fan");
    Integer grade = classRepository.findById(classId).map(SchoolClassEntity::getGrade).orElse(null);
    return grade == null ? subjectName : subjectName + " " + grade + "-sinf";
  }
}
