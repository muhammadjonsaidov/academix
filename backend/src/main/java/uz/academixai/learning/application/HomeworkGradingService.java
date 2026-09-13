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
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.HomeworkGrading;
import uz.academixai.learning.application.port.out.GradeStore;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionStore;
import uz.academixai.learning.application.port.out.StudentAchievementUpdater;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;

/** Final teacher grade command, including status transition and Progress update. */
@Service
public class HomeworkGradingService implements HomeworkGrading {

  private final HomeworkSubmissionReadRepository submissionReads;
  private final HomeworkSubmissionStore submissionWrites;
  private final HomeworkAssignmentStore assignments;
  private final SubmissionAssessmentLookup assessments;
  private final GradeStore grades;
  private final StudentAchievementUpdater achievementUpdater;

  public HomeworkGradingService(
      HomeworkSubmissionReadRepository submissionReads,
      HomeworkSubmissionStore submissionWrites,
      HomeworkAssignmentStore assignments,
      SubmissionAssessmentLookup assessments,
      GradeStore grades,
      StudentAchievementUpdater achievementUpdater) {
    this.submissionReads = submissionReads;
    this.submissionWrites = submissionWrites;
    this.assignments = assignments;
    this.assessments = assessments;
    this.grades = grades;
    this.achievementUpdater = achievementUpdater;
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
    boolean overridden = feedback != null && Math.abs(aiScore - score) > 0.01f;
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
            submission.studentTaskId(),
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
    return saved;
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_HW_NOT_FOUND",
        "Topshiriq topilmadi.",
        "ID ni tekshiring yoki sahifani yangilang.");
  }
}
