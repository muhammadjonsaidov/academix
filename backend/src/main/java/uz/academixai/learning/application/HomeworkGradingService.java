package uz.academixai.learning.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.learning.application.port.in.HomeworkGrading;
import uz.academixai.learning.application.port.out.GradeStore;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionStore;
import uz.academixai.learning.application.port.out.LearningNotifier;
import uz.academixai.learning.application.port.out.StudentAchievementUpdater;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;
import uz.academixai.learning.domain.FivePointScale;
import uz.academixai.learning.domain.ReviewQueueOrder;
import uz.academixai.shared.error.ApiException;

/** Final teacher grade command, including status transition and Progress update. */
@Service
public class HomeworkGradingService implements HomeworkGrading {

  private final HomeworkSubmissionReadRepository submissionReads;
  private final HomeworkSubmissionStore submissionWrites;
  private final HomeworkAssignmentStore assignments;
  private final SubmissionAssessmentLookup assessments;
  private final GradeStore grades;
  private final StudentAchievementUpdater achievementUpdater;
  private final LearningNotifier notifier;

  public HomeworkGradingService(
      HomeworkSubmissionReadRepository submissionReads,
      HomeworkSubmissionStore submissionWrites,
      HomeworkAssignmentStore assignments,
      SubmissionAssessmentLookup assessments,
      GradeStore grades,
      StudentAchievementUpdater achievementUpdater,
      LearningNotifier notifier) {
    this.submissionReads = submissionReads;
    this.submissionWrites = submissionWrites;
    this.assignments = assignments;
    this.assessments = assessments;
    this.grades = grades;
    this.achievementUpdater = achievementUpdater;
    this.notifier = notifier;
  }

  @Override
  public Grade grade(
      UUID schoolId,
      UUID teacherId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment,
      boolean isExcellent) {
    HomeworkSubmission submission =
        submissionReads
            .findByIdAndSchoolId(submissionId, schoolId)
            .orElseThrow(HomeworkGradingService::notFound);
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(submission.assignmentId(), schoolId)
            .orElseThrow(HomeworkGradingService::notFound);
    if (!assignment.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi baholashi mumkin.");
    }
    AIFeedback feedback = assessments.feedback(submissionId).orElse(null);
    float aiScore = feedback == null ? 0f : feedback.aiScorePercent();
    boolean regraded = grades.findBySubmissionId(submissionId).isPresent();
    // Scores are whole numbers; confirming 88.4% as 88 is agreeing with the AI, not overriding it.
    boolean overridden = feedback != null && Math.round(aiScore) != score;
    Grade saved =
        grades.save(
            new Grade(
                grades.findBySubmissionId(submissionId).map(Grade::id).orElseGet(UUID::randomUUID),
                submissionId,
                teacherId,
                score,
                fivePointGrade,
                teacherComment,
                overridden,
                aiScore,
                LocalDateTime.now()));
    submissionWrites.save(
        new HomeworkSubmission(
            submission.id(),
            submission.schoolId(),
            submission.assignmentId(),
            submission.studentId(),
            submission.type(),
            submission.textContent(),
            submission.imageUrl(),
            SubmissionStatus.GRADED,
            submission.isLate(),
            submission.submittedAt(),
            submission.xpEarned()));
    achievementUpdater.applyGrade(
        submissionId, submission.studentId(), score, submission.isLate(), isExcellent);
    notifier.homeworkGraded(
        submission.studentId(), submissionId, assignment.title(), score, fivePointGrade, regraded);
    return saved;
  }

  @Override
  public int approveClean(UUID schoolId, UUID teacherId, UUID assignmentId) {
    HomeworkAssignment assignment =
        assignments
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(HomeworkGradingService::notFound);
    if (!assignment.teacherId().equals(teacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
          "Faqat vazifani yaratgan o'qituvchi baholashi mumkin.");
    }
    int approved = 0;
    for (HomeworkSubmission submission : submissionReads.findByAssignmentId(assignmentId)) {
      if (submission.status() != SubmissionStatus.AI_DONE) {
        continue;
      }
      AIFeedback feedback = assessments.feedback(submission.id()).orElse(null);
      if (feedback == null || ReviewQueueOrder.needsCloseLook(feedback)) {
        continue;
      }
      int score = Math.round(feedback.aiScorePercent());
      grade(
          schoolId,
          teacherId,
          submission.id(),
          score,
          FivePointScale.fromPercent(score),
          null,
          false);
      approved++;
    }
    return approved;
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Topshiriq topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }
}
