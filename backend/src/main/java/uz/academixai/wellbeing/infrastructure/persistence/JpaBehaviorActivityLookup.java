package uz.academixai.wellbeing.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.AiChatMessageEntity;
import uz.academixai.infrastructure.persistence.AiChatMessageRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;
import uz.academixai.shared.tenancy.TenantScope;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;

/**
 * RLS-aware adapter for the activity metadata used by a scheduled Wellbeing job.
 *
 * <p>The scheduler has no request transaction, so the adapter declares the student's own tenant
 * scope through {@link TenantScope} — the same mechanism HTTP requests receive from the RLS filter
 * — before querying protected tables.
 */
@Repository
public class JpaBehaviorActivityLookup implements BehaviorActivityLookup {

  private final HomeworkSubmissionRepository homeworkSubmissions;
  private final ExamSubmissionRepository examSubmissions;
  private final AiChatMessageRepository chatMessages;
  private final XpHistoryRepository xpHistory;
  private final StudentProfileRepository students;
  private final TenantScope tenantScope;

  public JpaBehaviorActivityLookup(
      HomeworkSubmissionRepository homeworkSubmissions,
      ExamSubmissionRepository examSubmissions,
      AiChatMessageRepository chatMessages,
      XpHistoryRepository xpHistory,
      StudentProfileRepository students,
      TenantScope tenantScope) {
    this.homeworkSubmissions = homeworkSubmissions;
    this.examSubmissions = examSubmissions;
    this.chatMessages = chatMessages;
    this.xpHistory = xpHistory;
    this.students = students;
    this.tenantScope = tenantScope;
  }

  @Override
  public Activity find(UUID schoolId, UUID studentId, LocalDateTime since, int maxChatMessages) {
    Activity snapshot =
        tenantScope.callAsTenant(
            schoolId,
            studentId,
            () -> {
              List<LocalDateTime> submissionTimes = new ArrayList<>();
              homeworkSubmissions.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                  .map(submission -> submission.toDomain().submittedAt())
                  .filter(time -> time.isAfter(since))
                  .forEach(submissionTimes::add);
              examSubmissions.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
                  .map(submission -> submission.toDomain().uploadedAt())
                  .filter(time -> time.isAfter(since))
                  .forEach(submissionTimes::add);
              List<XpHistoryEntry> recentXpHistory =
                  xpHistory.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
                      .map(entry -> entry.toDomain())
                      .filter(entry -> entry.occurredAt().isAfter(since))
                      .toList();
              long recentXp = recentXpHistory.stream().mapToLong(XpHistoryEntry::xp).sum();
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
                  recentXpHistory,
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
