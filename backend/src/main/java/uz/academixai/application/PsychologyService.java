package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.academixai.domain.NotificationType;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.Role;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.infrastructure.ai.PsychologyAnalysisResult;
import uz.academixai.infrastructure.ai.PsychologySignalCandidate;
import uz.academixai.infrastructure.ai.QwenAIClient;
import uz.academixai.infrastructure.ai.QwenUnavailableException;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.infrastructure.persistence.XpHistoryRepository;

/**
 * academix_tz.md §1.14/§3.3/§4 PsychologyService — nightly silent behavior analysis + the strict
 * severity notify matrix.
 *
 * <p><b>Real, flagged scope limit (not silently skipped):</b> TZ §3.3's prompt asks for analysis of
 * "kirish vaqtlari, AI chat bilan yozishmalari" (login times + AI chat transcripts), but neither
 * input actually exists in this codebase yet — there's no login-history table (only a single {@code
 * users.last_login_at} timestamp), and the AI Tutor chat feature itself (§2.4/§3.4, {@code
 * ai_chat_messages}) hasn't been built (no entity/migration anywhere). Building either is a
 * separate, sprint-sized feature on its own. This implementation substitutes the activity signal
 * that IS real and available — homework/exam submission timestamps, XP history, and {@code
 * last_submission_date} — which still supports 4 of the 7 {@link SignalType} values
 * (LATE_NIGHT_ACTIVITY, MOTIVATION_DROP, SUDDEN_PERFORMANCE_DROP, SUBMISSION_STOP) meaningfully.
 * The 3 chat-dependent types (NEGATIVE_LANGUAGE, AGGRESSIVE_LANGUAGE, MANIPULATION_ATTEMPT) simply
 * won't ever fire until the AI Tutor chat feature lands — Qwen is still told the exact same
 * response contract (§3.3) and may return any of the 7 types; the ones needing chat context just
 * won't have real evidence to draw on with an empty activity summary for that portion.
 *
 * <p><b>Second real, flagged gap:</b> CRITICAL severity's notify matrix requires notifying the
 * parent, but no {@code parent_student_links} table exists anywhere in this codebase (confirmed —
 * {@link SchoolContextResolver}'s own Javadoc already documents PARENT as unresolved). {@code
 * notifiedParent} is therefore always left {@code false} for now — the notify matrix code path
 * exists and is ready, it just has no real recipient to resolve yet.
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
  private final XpHistoryRepository xpHistoryRepository;
  private final SchoolClassRepository classRepository;
  private final UserRepository userRepository;
  private final PsychologicalSignalRepository signalRepository;
  private final NotificationService notificationService;
  private final QwenAIClient qwenAIClient;
  private final ObjectMapper objectMapper;

  public PsychologyService(
      StudentProfileRepository studentProfileRepository,
      HomeworkSubmissionRepository homeworkSubmissionRepository,
      ExamSubmissionRepository examSubmissionRepository,
      XpHistoryRepository xpHistoryRepository,
      SchoolClassRepository classRepository,
      UserRepository userRepository,
      PsychologicalSignalRepository signalRepository,
      NotificationService notificationService,
      QwenAIClient qwenAIClient,
      ObjectMapper objectMapper) {
    this.studentProfileRepository = studentProfileRepository;
    this.homeworkSubmissionRepository = homeworkSubmissionRepository;
    this.examSubmissionRepository = examSubmissionRepository;
    this.xpHistoryRepository = xpHistoryRepository;
    this.classRepository = classRepository;
    this.userRepository = userRepository;
    this.signalRepository = signalRepository;
    this.notificationService = notificationService;
    this.qwenAIClient = qwenAIClient;
    this.objectMapper = objectMapper;
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
    String activitySummary = buildActivitySummary(studentId);
    PsychologyAnalysisResult result;
    try {
      result = qwenAIClient.analyzePsychology(activitySummary);
    } catch (QwenUnavailableException e) {
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
    boolean notifyParent = severity == SignalSeverity.CRITICAL;

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
            false, // notifiedParent — see class Javadoc, no parent_student_links table exists yet
            notifyTeacherAndPsychologist,
            false,
            LocalDateTime.now(),
            null);
    PsychologicalSignal saved =
        signalRepository.save(PsychologicalSignalEntity.fromDomain(signal)).toDomain();

    if (notifyTeacherAndPsychologist) {
      notifyClassTeacher(studentId, type, severity);
      notifyPsychologists(studentId, type, severity);
    }
    if (notifyParent) {
      // Intentionally a no-op today — see class Javadoc's second flagged gap. The matrix branch
      // exists so wiring in a real parent_student_links resolution later is a one-line change,
      // not a rediscovery of this whole method.
      log.warn(
          "CRITICAL psychological signal for student {} would notify parent, but no"
              + " parent_student_links resolution exists yet — skipped, flagged not silent.",
          studentId);
    }
    return saved;
  }

  private void notifyClassTeacher(UUID studentId, SignalType type, SignalSeverity severity) {
    studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::getClassId)
        .flatMap(
            classId -> classRepository.findById(classId).map(SchoolClassEntity::getClassTeacherId))
        .ifPresent(
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
                        severity.name())));
  }

  private void notifyPsychologists(UUID studentId, SignalType type, SignalSeverity severity) {
    studentProfileRepository
        .findByUserId(studentId)
        .map(StudentProfileEntity::getSchoolId)
        .ifPresent(
            schoolId ->
                userRepository
                    .findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(
                        Role.PSYCHOLOGIST, schoolId)
                    .forEach(
                        psychologist ->
                            notificationService.sendNotification(
                                psychologist.getId(),
                                NotificationType.PSYCHOLOGICAL_ALERT,
                                "Psixologik signal",
                                "O'quvchida %s (%s) darajali signal aniqlandi."
                                    .formatted(type, severity),
                                Map.of(
                                    "studentId", studentId.toString(),
                                    "type", type.name(),
                                    "severity", severity.name()))));
  }

  private String buildActivitySummary(UUID studentId) {
    LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
    List<LocalDateTime> submissionTimes = new ArrayList<>();
    homeworkSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
        .filter(s -> s.toDomain().submittedAt().isAfter(since))
        .forEach(s -> submissionTimes.add(s.toDomain().submittedAt()));
    examSubmissionRepository.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
        .filter(s -> s.toDomain().uploadedAt().isAfter(since))
        .forEach(s -> submissionTimes.add(s.toDomain().uploadedAt()));

    long nightSubmissions = submissionTimes.stream().filter(PsychologyService::isNightTime).count();

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
        Shu davrda jami %d XP to'pladi. AI chat yozishmalari mavjud emas (bu funksiya hali \
        ishga tushirilmagan)."""
        .formatted(
            LOOKBACK_DAYS,
            submissionTimes.size(),
            nightSubmissions,
            daysSinceLastSubmission,
            recentXp);
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
