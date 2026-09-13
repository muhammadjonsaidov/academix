package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.application.port.out.ai.AiProviderUnavailableException;
import uz.academixai.application.port.out.ai.PsychologyAnalysisResult;
import uz.academixai.application.port.out.ai.PsychologySignalCandidate;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.infrastructure.persistence.AiChatMessageEntity;
import uz.academixai.infrastructure.persistence.AiChatMessageRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.ParentStudentLinkEntity;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.notification.application.NotificationService;
import uz.academixai.notification.domain.NotificationType;
import uz.academixai.progress.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;

/**
 * academix_tz.md §1.14/§3.3/§4 PsychologyService — nightly silent behavior analysis + the strict
 * severity notify matrix.
 *
 * <p><b>Real, flagged scope limit (partially closed in Sprint 12):</b> TZ §3.3's prompt asks for
 * analysis of "kirish vaqtlari, AI chat bilan yozishmalari" (login times + AI chat transcripts).
 * Chat transcripts are now real — {@code ai_chat_messages} (§2.3/§3.4) shipped in Sprint 12, and
 * {@link #buildChatTranscriptSummary} feeds the student's own recent chat messages into the same
 * Qwen call, so NEGATIVE_LANGUAGE/AGGRESSIVE_LANGUAGE/MANIPULATION_ATTEMPT can now actually fire
 * when there's real evidence. Login-time analysis is still a gap — there's no login-history table
 * (only a single {@code users.last_login_at} timestamp, no per-login record), so
 * LATE_NIGHT_ACTIVITY still infers from submission timestamps instead, which remains a reasonable
 * proxy but not the literal spec'd signal.
 *
 * <p><b>Sprint 8 update:</b> CRITICAL severity's parent notification is now wired to real {@code
 * parent_student_links} rows — previously flagged as a no-op gap (no such table existed). A student
 * with no linked parent still can't be notified (logged, not silent) — that's a genuine "this
 * student has no parent account linked yet" state, not a missing feature.
 */
@Service
public class PsychologyService {

  private static final int LOOKBACK_DAYS = 14;
  private static final LocalTime NIGHT_START = LocalTime.of(23, 0);
  private static final LocalTime NIGHT_END = LocalTime.of(5, 0);
  private static final Logger log = LoggerFactory.getLogger(PsychologyService.class);

  private final StudentProfileRepository studentProfileRepository;
  private final HomeworkSubmissionRepository homeworkSubmissionRepository;
  private final ExamSubmissionRepository examSubmissionRepository;
  private final AiChatMessageRepository aiChatMessageRepository;
  private final XpHistoryRepository xpHistoryRepository;
  private final SchoolClassRepository classRepository;
  private final UserRepository userRepository;
  private final PsychologicalSignalRepository signalRepository;
  private final ParentStudentLinkRepository parentStudentLinkRepository;
  private final NotificationService notificationService;
  private final AiProvider aiClient;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;
  private final EntityManager entityManager;

  public PsychologyService(
      StudentProfileRepository studentProfileRepository,
      HomeworkSubmissionRepository homeworkSubmissionRepository,
      ExamSubmissionRepository examSubmissionRepository,
      AiChatMessageRepository aiChatMessageRepository,
      XpHistoryRepository xpHistoryRepository,
      SchoolClassRepository classRepository,
      UserRepository userRepository,
      PsychologicalSignalRepository signalRepository,
      ParentStudentLinkRepository parentStudentLinkRepository,
      NotificationService notificationService,
      AiProvider aiClient,
      ObjectMapper objectMapper,
      TransactionTemplate transactionTemplate,
      EntityManager entityManager) {
    this.studentProfileRepository = studentProfileRepository;
    this.homeworkSubmissionRepository = homeworkSubmissionRepository;
    this.examSubmissionRepository = examSubmissionRepository;
    this.aiChatMessageRepository = aiChatMessageRepository;
    this.xpHistoryRepository = xpHistoryRepository;
    this.classRepository = classRepository;
    this.userRepository = userRepository;
    this.signalRepository = signalRepository;
    this.parentStudentLinkRepository = parentStudentLinkRepository;
    this.notificationService = notificationService;
    this.aiClient = aiClient;
    this.objectMapper = objectMapper;
    this.transactionTemplate = transactionTemplate;
    this.entityManager = entityManager;
  }

  // Nightly at 23:00, matching backend_tdd.md/TZ §4's literal schedule.
  @Scheduled(cron = "0 0 23 * * *")
  public void analyzeAllStudentsBehavior() {
    List<StudentProfileEntity> students = studentProfileRepository.findAll();
    int totalSignals = 0;
    for (StudentProfileEntity student : students) {
      if (!student.isActive()) {
        continue;
      }
      totalSignals += analyzeStudentBehavior(student.getUserId()).size();
    }
    log.info(
        "Nightly psychology analysis: {} students, {} signals created",
        students.size(),
        totalSignals);
  }

  public List<PsychologicalSignal> analyzeStudentBehavior(UUID studentId) {
    Optional<UUID> schoolId =
        studentProfileRepository.findByUserId(studentId).map(StudentProfileEntity::getSchoolId);
    if (schoolId.isEmpty()) {
      log.warn(
          "Psychological analysis skipped for student {}: no school_id on profile.", studentId);
      return List.of();
    }
    String activitySummary = buildActivitySummary(studentId, schoolId.get());
    PsychologyAnalysisResult result;
    try {
      result = aiClient.analyzePsychology(activitySummary);
    } catch (AiProviderUnavailableException e) {
      // Graceful degradation, same principle as AIAnalysisService — a Qwen outage never blocks
      // the nightly job for other students, it just means no signal fires for this one tonight.
      log.warn("Psychology analysis unavailable for student {}", studentId, e);
      return List.of();
    }

    List<PsychologicalSignal> created = new ArrayList<>();
    for (PsychologySignalCandidate candidate : result.signals()) {
      SignalType type = parseType(candidate.type());
      SignalSeverity severity = parseSeverity(candidate.severity());
      if (type == null || severity == null) {
        continue;
      }
      boolean manipulation = detectManipulationAttempt(result.isManipulationSuspected(), type);
      created.add(
          createSignalAndNotify(studentId, type, severity, candidate.evidence(), manipulation));
    }
    return created;
  }

  /**
   * TZ §4 lists {@code detectManipulationAttempt(studentId, messages)} as its own method, but the
   * one real signal for it (Qwen's {@code isManipulationSuspected} flag) already comes back in the
   * same response {@link #analyzeStudentBehavior} already made — a second independent Qwen call
   * with the same chat transcript would just re-ask the same question. This applies that flag to
   * any {@code MANIPULATION_ATTEMPT}-typed candidate from that one response instead of spending a
   * second AI call to re-derive it.
   */
  private static boolean detectManipulationAttempt(
      boolean isManipulationSuspected, SignalType type) {
    return isManipulationSuspected && type == SignalType.MANIPULATION_ATTEMPT;
  }

  /**
   * academix_tz.md §1.14's severity → notify matrix (CLAUDE.md "Backend architecture"), applied
   * strictly: LOW → log/watchlist only, no notify. MEDIUM/HIGH → class teacher + psychologist.
   * CRITICAL → + parent. {@code isManipulation} never changes routing, only the flag on the row.
   *
   * <p>Deviates from TZ §4's 3-arg signature ({@code studentId, type, severity}) by requiring a
   * description/evidence and the manipulation flag too — a real signal can't be created with no
   * content, and the spec's own {@link PsychologicalSignal} entity has no way to fill those in
   * otherwise.
   */
  public PsychologicalSignal createSignalAndNotify(
      UUID studentId,
      SignalType type,
      SignalSeverity severity,
      String evidence,
      boolean isManipulation) {
    boolean notifyTeacherAndPsychologist =
        severity == SignalSeverity.MEDIUM
            || severity == SignalSeverity.HIGH
            || severity == SignalSeverity.CRITICAL;
    boolean shouldNotifyParent = severity == SignalSeverity.CRITICAL;
    boolean parentNotified = shouldNotifyParent && notifyParent(studentId, type, severity);

    PsychologicalSignal signal =
        new PsychologicalSignal(
            UUID.randomUUID(),
            studentId,
            type,
            severity,
            evidence,
            toJson(evidence),
            isManipulation,
            notifyTeacherAndPsychologist,
            parentNotified,
            notifyTeacherAndPsychologist,
            false,
            LocalDateTime.now(),
            null,
            null,
            null);
    PsychologicalSignal saved =
        signalRepository.save(PsychologicalSignalEntity.fromDomain(signal)).toDomain();

    if (notifyTeacherAndPsychologist) {
      notifyClassTeacher(studentId, type, severity);
      notifyPsychologists(studentId, type, severity);
    }
    if (shouldNotifyParent && !parentNotified) {
      // Real, flagged case: this student has no active parent_student_links row yet (no admin
      // has linked a parent to them) — CRITICAL severity still can't reach a parent who was
      // never linked. Not silently skipped: logged so it's visible in ops.
      log.warn(
          "CRITICAL psychological signal for student {} has no linked parent to notify.",
          studentId);
    }
    return saved;
  }

  /**
   * Sprint 8 — resolves real parent(s) via {@code parent_student_links}, unblocking Sprint 7's
   * flagged gap. Returns whether at least one parent was actually notified.
   */
  private boolean notifyParent(UUID studentId, SignalType type, SignalSeverity severity) {
    List<ParentStudentLinkEntity> links =
        parentStudentLinkRepository.findByStudentUserIdAndIsActiveTrue(studentId);
    for (ParentStudentLinkEntity link : links) {
      notificationService.sendNotification(
          link.getParentUserId(),
          NotificationType.PSYCHOLOGICAL_ALERT,
          "Diqqat talab qiluvchi holat",
          "Farzandingizda e'tibor talab qiluvchi holat aniqlandi. Batafsil ma'lumot uchun"
              + " maktab psixologi bilan bog'laning.",
          Map.of(
              "studentId", studentId.toString(), "type", type.name(), "severity", severity.name()));
    }
    return !links.isEmpty();
  }

  private void notifyClassTeacher(UUID studentId, SignalType type, SignalSeverity severity) {
    studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::getClassId)
        .flatMap(
            classId -> classRepository.findById(classId).map(SchoolClassEntity::getClassTeacherId))
        .ifPresentOrElse(
            teacherId ->
                notificationService.sendNotification(
                    teacherId,
                    NotificationType.PSYCHOLOGICAL_ALERT,
                    "Psixologik signal",
                    "O'quvchida %s (%s) darajali signal aniqlandi.".formatted(type, severity),
                    Map.of(
                        "studentId",
                        studentId.toString(),
                        "type",
                        type.name(),
                        "severity",
                        severity.name())),
            // Real, flagged case (same principle as the parent-notify gap already logged
            // below): a student with no classId, or a class with no classTeacherId set, means
            // a MEDIUM+ signal silently reaches no teacher — was previously a silent no-op.
            () ->
                log.warn(
                    "Psychological signal for student {} has no class teacher to notify"
                        + " (missing classId or classTeacherId).",
                    studentId));
  }

  private void notifyPsychologists(UUID studentId, SignalType type, SignalSeverity severity) {
    Optional<UUID> schoolId =
        studentProfileRepository.findByUserId(studentId).map(StudentProfileEntity::getSchoolId);
    if (schoolId.isEmpty()) {
      log.warn(
          "Psychological signal for student {} has no school to resolve psychologists for.",
          studentId);
      return;
    }
    List<UserEntity> psychologists =
        userRepository.findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(
            Role.PSYCHOLOGIST, schoolId.get());
    if (psychologists.isEmpty()) {
      log.warn("Psychological signal for student {} has no psychologists to notify.", studentId);
      return;
    }
    psychologists.forEach(
        psychologist ->
            notificationService.sendNotification(
                psychologist.getId(),
                NotificationType.PSYCHOLOGICAL_ALERT,
                "Psixologik signal",
                "O'quvchida %s (%s) darajali signal aniqlandi.".formatted(type, severity),
                Map.of(
                    "studentId", studentId.toString(),
                    "type", type.name(),
                    "severity", severity.name())));
  }

  /**
   * Runs its own {@code SET LOCAL app.current_school_id}, same as {@code RlsTransactionFilter} does
   * for HTTP requests and {@code HomeworkSubmissionListener} does for its queue. This method is
   * called from the {@code @Scheduled} nightly job (no HTTP request, no filter) and touches 3
   * RLS-enabled tables (homework_submissions, exam_submissions, ai_chat_messages) — confirmed
   * necessary by a real {@code unrecognized configuration parameter "app.current_school_id"}
   * failure the first time this job ever actually ran end-to-end.
   */
  private String buildActivitySummary(UUID studentId, UUID schoolId) {
    return transactionTemplate.execute(
        status -> {
          // SET LOCAL doesn't accept JDBC bind parameters, so the UUID is inlined directly —
          // safe here because schoolId always originated from student_profiles.school_id (a
          // real column value), never raw user input.
          entityManager
              .createNativeQuery("SET LOCAL app.current_school_id = '" + schoolId + "'")
              .executeUpdate();

          LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
          List<LocalDateTime> submissionTimes = new ArrayList<>();
          homeworkSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
              .filter(s -> s.toDomain().submittedAt().isAfter(since))
              .forEach(s -> submissionTimes.add(s.toDomain().submittedAt()));
          examSubmissionRepository.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
              .filter(s -> s.toDomain().uploadedAt().isAfter(since))
              .forEach(s -> submissionTimes.add(s.toDomain().uploadedAt()));

          long nightSubmissions =
              submissionTimes.stream().filter(PsychologyService::isNightTime).count();

          List<XpHistoryEntity> xpHistory =
              xpHistoryRepository.findByStudentIdOrderByOccurredAtDesc(studentId);
          long recentXp =
              xpHistory.stream()
                  .filter(x -> x.toDomain().occurredAt().isAfter(since))
                  .mapToLong(x -> x.toDomain().xp())
                  .sum();

          LocalDate lastSubmissionDate =
              studentProfileRepository
                  .findByUserId(studentId)
                  .map(StudentProfileEntity::toDomain)
                  .map(p -> p.lastSubmissionDate())
                  .orElse(null);
          long daysSinceLastSubmission =
              lastSubmissionDate == null
                  ? LOOKBACK_DAYS
                  : java.time.temporal.ChronoUnit.DAYS.between(lastSubmissionDate, LocalDate.now());

          return """
              Oxirgi %d kunlik faollik: jami %d marta topshiriq yubordi, shulardan %d marotaba \
              tungi soat 23:00-05:00 oralig'ida. Oxirgi topshiriqdan beri %d kun o'tdi. \
              Shu davrda jami %d XP to'pladi.

              %s"""
              .formatted(
                  LOOKBACK_DAYS,
                  submissionTimes.size(),
                  nightSubmissions,
                  daysSinceLastSubmission,
                  recentXp,
                  buildChatTranscriptSummary(studentId, since));
        });
  }

  // Sprint 12 added AI Tutor chat (ai_chat_messages) — wired in here now that real data exists,
  // closing the gap this class's own Javadoc previously flagged. Only the student's own messages
  // are included (not AI responses) since tone/language is what NEGATIVE_LANGUAGE/
  // AGGRESSIVE_LANGUAGE/MANIPULATION_ATTEMPT need to detect; capped at 20 most recent messages in
  // the lookback window to keep the prompt bounded.
  private String buildChatTranscriptSummary(UUID studentId, LocalDateTime since) {
    List<AiChatMessageEntity> messages =
        aiChatMessageRepository.findByStudentIdOrderByCreatedAtDesc(studentId, Limit.of(20));
    List<String> recentMessages =
        messages.stream()
            .map(AiChatMessageEntity::toDomain)
            .filter(m -> m.createdAt().isAfter(since))
            .map(m -> "- " + m.message())
            .toList();
    if (recentMessages.isEmpty()) {
      return "AI chat yozishmalari: shu davrda yo'q.";
    }
    return "AI chat orqali yuborilgan xabarlar:\n" + String.join("\n", recentMessages);
  }

  private static boolean isNightTime(LocalDateTime dateTime) {
    LocalTime time = dateTime.toLocalTime();
    return time.isAfter(NIGHT_START) || time.isBefore(NIGHT_END);
  }

  private String toJson(String evidence) {
    try {
      return objectMapper.writeValueAsString(Map.of("evidence", evidence == null ? "" : evidence));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize signal evidence", e);
    }
  }

  private static SignalType parseType(String raw) {
    try {
      return SignalType.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }

  private static SignalSeverity parseSeverity(String raw) {
    try {
      return SignalSeverity.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}
