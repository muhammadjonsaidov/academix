package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Topshirilgan ishlarni ko'rish va baholash" — teacher views AI-graded
 * submissions and can approve/override the score.
 *
 * <p>Scope: only submissions for assignments this teacher created are visible/gradable — same
 * ownership boundary as {@link HomeworkService}'s update/delete.
 */
@Service
public class TeacherSubmissionService {

  private final HomeworkSubmissionRepository submissionRepository;
  private final HomeworkAssignmentRepository assignmentRepository;
  private final AIFeedbackRepository aiFeedbackRepository;
  private final GradeRepository gradeRepository;
  private final UserRepository userRepository;
  private final XPService xpService;

  public TeacherSubmissionService(
      HomeworkSubmissionRepository submissionRepository,
      HomeworkAssignmentRepository assignmentRepository,
      AIFeedbackRepository aiFeedbackRepository,
      GradeRepository gradeRepository,
      UserRepository userRepository,
      XPService xpService) {
    this.submissionRepository = submissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.aiFeedbackRepository = aiFeedbackRepository;
    this.gradeRepository = gradeRepository;
    this.userRepository = userRepository;
    this.xpService = xpService;
  }

  public record SubmissionWithFeedback(
      HomeworkSubmission submission,
      String studentName,
      AIFeedbackEntity feedback,
      GradeEntity grade) {}

  public List<SubmissionWithFeedback> list(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID classId, SubmissionStatus status) {
    List<HomeworkSubmissionEntity> submissions;
    if (assignmentId != null) {
      requireOwnedAssignment(schoolId, teacherId, assignmentId);
      submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    } else {
      List<HomeworkAssignmentEntity> myAssignments =
          assignmentRepository.findBySchoolIdAndTeacherIdOrderByDeadlineAtDesc(schoolId, teacherId);
      List<UUID> ids =
          myAssignments.stream()
              .filter(a -> classId == null || a.getClassId().equals(classId))
              .map(HomeworkAssignmentEntity::getId)
              .toList();
      submissions =
          ids.isEmpty()
              ? List.of()
              : submissionRepository.findByAssignmentIdInOrderBySubmittedAtDesc(ids);
    }

    return submissions.stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .filter(s -> status == null || s.status() == status)
        .map(
            s ->
                new SubmissionWithFeedback(
                    s,
                    studentName(s.studentId()),
                    aiFeedbackRepository.findBySubmissionId(s.id()).orElse(null),
                    gradeRepository.findBySubmissionId(s.id()).orElse(null)))
        .toList();
  }

  public SubmissionWithFeedback get(UUID schoolId, UUID teacherId, UUID submissionId) {
    HomeworkSubmissionEntity subEntity = requireSubmission(schoolId, submissionId);
    requireOwnedAssignment(schoolId, teacherId, subEntity.getAssignmentId());
    HomeworkSubmission submission = subEntity.toDomain();
    return new SubmissionWithFeedback(
        submission,
        studentName(submission.studentId()),
        aiFeedbackRepository.findBySubmissionId(submissionId).orElse(null),
        gradeRepository.findBySubmissionId(submissionId).orElse(null));
  }

  private String studentName(UUID studentId) {
    return userRepository
        .findById(studentId)
        .map(u -> u.getFirstName() + " " + u.getLastName())
        .orElse("");
  }

  public Grade grade(
      UUID schoolId,
      UUID teacherId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment,
      boolean isExcellent) {
    HomeworkSubmissionEntity subEntity = requireSubmission(schoolId, submissionId);
    requireOwnedAssignment(schoolId, teacherId, subEntity.getAssignmentId());

    AIFeedbackEntity aiFeedback =
        aiFeedbackRepository.findBySubmissionId(submissionId).orElse(null);
    float aiOriginalScore = aiFeedback == null ? 0f : aiFeedback.toDomain().aiScorePercent();
    boolean overrode = aiFeedback != null && Math.abs(aiOriginalScore - score) > 0.01f;

    GradeEntity existing = gradeRepository.findBySubmissionId(submissionId).orElse(null);
    UUID gradeId = existing != null ? existing.toDomain().id() : UUID.randomUUID();
    Grade domainGrade =
        new Grade(
            gradeId,
            submissionId,
            teacherId,
            score,
            fivePointGrade,
            teacherComment,
            overrode,
            aiOriginalScore,
            LocalDateTime.now());
    Grade saved = gradeRepository.save(GradeEntity.fromDomain(domainGrade)).toDomain();

    HomeworkSubmission submission = subEntity.toDomain();
    HomeworkSubmission graded =
        new HomeworkSubmission(
            submission.id(),
            submission.schoolId(),
            submission.assignmentId(),
            submission.studentTaskId(),
            submission.studentId(),
            submission.type(),
            submission.textContent(),
            submission.imageUrl(),
            SubmissionStatus.GRADED,
            submission.isLate(),
            submission.submittedAt(),
            submission.xpEarned());
    submissionRepository.save(HomeworkSubmissionEntity.fromDomain(graded));

    // academix_tz.md §4 — GRADED always (re-)runs XP using the teacher's score, not the AI's
    // (XPService.calculateAndAwardXP is delta-based per submissionId, so a submission already
    // XP'd at AI_DONE gets adjusted rather than double-awarded). isExcellent is a flat bonus on
    // top, independent of the tier.
    xpService.calculateAndAwardXP(submissionId, score, submission.isLate());
    xpService.updateStreak(submission.studentId(), score);
    if (isExcellent) {
      xpService.awardExcellentBonus(submission.studentId());
    }
    xpService.checkAndAwardBadges(submission.studentId());

    return saved;
  }

  private HomeworkSubmissionEntity requireSubmission(UUID schoolId, UUID submissionId) {
    return submissionRepository
        .findByIdAndSchoolId(submissionId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_HW_NOT_FOUND",
                    "Topshiriq topilmadi.",
                    "ID ni tekshiring yoki sahifani yangilang."));
  }

  private void requireOwnedAssignment(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignmentEntity assignment =
        assignmentRepository
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_HW_NOT_FOUND",
                        "Uy vazifasi topilmadi.",
                        "ID ni tekshiring yoki sahifani yangilang."));
    if (!assignment.getTeacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi ko'rishi mumkin.");
    }
  }
}
