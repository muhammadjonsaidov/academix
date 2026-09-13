package uz.academixai.wellbeing.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.infrastructure.persistence.AiChatMessageEntity;
import uz.academixai.infrastructure.persistence.AiChatMessageRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;

/**
 * RLS-aware adapter for the activity metadata used by a scheduled Wellbeing job.
 *
 * <p>The scheduler has no request transaction, so the adapter establishes the same local school
 * scope that HTTP requests receive from the RLS filter before querying protected tables.
 */
@Repository
public class JpaBehaviorActivityLookup implements BehaviorActivityLookup {

  private final HomeworkSubmissionRepository homeworkSubmissions;
  private final ExamSubmissionRepository examSubmissions;
  private final AiChatMessageRepository chatMessages;
  private final XpHistoryRepository xpHistory;
  private final StudentProfileRepository students;
  private final TransactionTemplate transactions;
  private final EntityManager entityManager;

  public JpaBehaviorActivityLookup(
      HomeworkSubmissionRepository homeworkSubmissions,
      ExamSubmissionRepository examSubmissions,
      AiChatMessageRepository chatMessages,
      XpHistoryRepository xpHistory,
      StudentProfileRepository students,
      TransactionTemplate transactions,
      EntityManager entityManager) {
    this.homeworkSubmissions = homeworkSubmissions;
    this.examSubmissions = examSubmissions;
    this.chatMessages = chatMessages;
    this.xpHistory = xpHistory;
    this.students = students;
    this.transactions = transactions;
    this.entityManager = entityManager;
  }

  @Override
  public Activity find(UUID schoolId, UUID studentId, LocalDateTime since, int maxChatMessages) {
    Activity snapshot =
        transactions.execute(
            status -> {
              entityManager
                  .createNativeQuery("SET LOCAL app.current_school_id = '" + schoolId + "'")
                  .executeUpdate();
              List<LocalDateTime> submissionTimes = new ArrayList<>();
              homeworkSubmissions.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                  .map(submission -> submission.toDomain().submittedAt())
                  .filter(time -> time.isAfter(since))
                  .forEach(submissionTimes::add);
              examSubmissions.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
                  .map(submission -> submission.toDomain().uploadedAt())
                  .filter(time -> time.isAfter(since))
                  .forEach(submissionTimes::add);
              long recentXp =
                  xpHistory.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
                      .map(entry -> entry.toDomain())
                      .filter(entry -> entry.occurredAt().isAfter(since))
                      .mapToLong(entry -> entry.xp())
                      .sum();
              List<String> recentChatMessages =
                  chatMessages
                      .findByStudentIdOrderByCreatedAtDesc(studentId, Limit.of(maxChatMessages))
                      .stream()
                      .map(AiChatMessageEntity::toDomain)
                      .filter(message -> message.createdAt().isAfter(since))
                      .map(message -> message.message())
                      .toList();
              return new Activity(
                  submissionTimes,
                  recentXp,
                  students
                      .findByUserId(studentId)
                      .map(StudentProfileEntity::toDomain)
                      .map(profile -> profile.lastSubmissionDate())
                      .orElse(null),
                  recentChatMessages);
            });
    if (snapshot == null) {
      throw new IllegalStateException("Could not read student activity in a transaction");
    }
    return snapshot;
  }
}
