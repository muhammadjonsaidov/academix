package uz.academixai.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.learning.application.port.out.GradeStore;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionStore;
import uz.academixai.learning.application.port.out.LearningNotifier;
import uz.academixai.learning.application.port.out.StudentAchievementUpdater;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;

class HomeworkGradingServiceTest {

  private final UUID schoolId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();
  private final UUID assignmentId = UUID.randomUUID();

  private final HomeworkSubmissionReadRepository reads =
      mock(HomeworkSubmissionReadRepository.class);
  private final HomeworkSubmissionStore writes = mock(HomeworkSubmissionStore.class);
  private final HomeworkAssignmentStore assignments = mock(HomeworkAssignmentStore.class);
  private final SubmissionAssessmentLookup assessments = mock(SubmissionAssessmentLookup.class);
  private final GradeStore grades = mock(GradeStore.class);
  private final StudentAchievementUpdater achievements = mock(StudentAchievementUpdater.class);
  private final LearningNotifier notifier = mock(LearningNotifier.class);

  private final HomeworkGradingService service =
      new HomeworkGradingService(
          reads, writes, assignments, assessments, grades, achievements, notifier);

  @Test
  void approvesOnlyCleanAiCheckedWorkAndLeavesFlaggedWorkForTheTeacher() {
    when(assignments.findByIdAndSchoolId(assignmentId, schoolId))
        .thenReturn(
            Optional.of(
                new HomeworkAssignment(
                    assignmentId,
                    schoolId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    teacherId,
                    "Kasrlar",
                    "",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    100,
                    true,
                    null)));
    HomeworkSubmission clean = submission(SubmissionStatus.AI_DONE);
    HomeworkSubmission flagged = submission(SubmissionStatus.AI_DONE);
    HomeworkSubmission stillProcessing = submission(SubmissionStatus.AI_PROCESSING);
    when(reads.findByAssignmentId(assignmentId))
        .thenReturn(List.of(clean, flagged, stillProcessing));
    for (HomeworkSubmission s : List.of(clean, flagged, stillProcessing)) {
      when(reads.findByIdAndSchoolId(s.id(), schoolId)).thenReturn(Optional.of(s));
    }
    when(assessments.feedback(clean.id()))
        .thenReturn(Optional.of(feedback(clean, 88.4f, PlagiarismType.CLEAN)));
    when(assessments.feedback(flagged.id()))
        .thenReturn(Optional.of(feedback(flagged, 95f, PlagiarismType.AI_GENERATED)));
    when(grades.findBySubmissionId(any())).thenReturn(Optional.empty());
    when(grades.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    int approved = service.approveClean(schoolId, teacherId, assignmentId);

    assertThat(approved).isEqualTo(1);
    ArgumentCaptor<Grade> saved = ArgumentCaptor.forClass(Grade.class);
    verify(grades).save(saved.capture());
    assertThat(saved.getValue().submissionId()).isEqualTo(clean.id());
    assertThat(saved.getValue().score()).isEqualTo(88);
    assertThat(saved.getValue().fivePointGrade()).isEqualTo(5);
    assertThat(saved.getValue().teacherOverrodeAI()).isFalse();
    verify(notifier)
        .homeworkGraded(
            eq(clean.studentId()), eq(clean.id()), eq("Kasrlar"), eq(88), eq(5), eq(false));
    verify(notifier, never())
        .homeworkGraded(
            eq(flagged.studentId()), any(), anyString(), anyInt(), anyInt(), anyBoolean());
  }

  private HomeworkSubmission submission(SubmissionStatus status) {
    return new HomeworkSubmission(
        UUID.randomUUID(),
        schoolId,
        assignmentId,
        UUID.randomUUID(),
        SubmissionType.TEXT,
        "javob",
        null,
        status,
        false,
        LocalDateTime.now(),
        0);
  }

  private static AIFeedback feedback(
      HomeworkSubmission submission, float score, PlagiarismType type) {
    return new AIFeedback(
        UUID.randomUUID(),
        submission.id(),
        "javob",
        1f,
        List.of(),
        List.of(),
        score,
        "",
        null,
        0f,
        type,
        LocalDateTime.now());
  }
}
