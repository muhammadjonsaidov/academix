package uz.academixai.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.WatchlistEntry;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.persistence.WatchlistEntryEntity;
import uz.academixai.infrastructure.persistence.WatchlistEntryRepository;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;

/**
 * academix_tz.md §2.6 "Psychologist API" — school-wide (not homeroom-limited, unlike the teacher
 * API) view over {@code psychological_signals}, scoped via the school every student in scope
 * belongs to (this table has no {@code school_id}/RLS — see {@link PsychologicalSignal}'s Javadoc).
 * {@code studentBehaviorProfile} is explicitly "DARS MAZMUNI YO'Q — faqat xulq-atvor" — never
 * touches grades/homework content, only activity metadata.
 */
@Service
public class PsychologistService {

  private static final int TREND_DAYS = 14;

  private final PsychologicalSignalRepository signalRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository classRepository;
  private final UserRepository userRepository;
  private final WatchlistEntryRepository watchlistRepository;
  private final HomeworkSubmissionRepository homeworkSubmissionRepository;
  private final ExamSubmissionRepository examSubmissionRepository;
  private final XpHistoryRepository xpHistoryRepository;

  public PsychologistService(
      PsychologicalSignalRepository signalRepository,
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository classRepository,
      UserRepository userRepository,
      WatchlistEntryRepository watchlistRepository,
      HomeworkSubmissionRepository homeworkSubmissionRepository,
      ExamSubmissionRepository examSubmissionRepository,
      XpHistoryRepository xpHistoryRepository) {
    this.signalRepository = signalRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.classRepository = classRepository;
    this.userRepository = userRepository;
    this.watchlistRepository = watchlistRepository;
    this.homeworkSubmissionRepository = homeworkSubmissionRepository;
    this.examSubmissionRepository = examSubmissionRepository;
    this.xpHistoryRepository = xpHistoryRepository;
  }

  public record Dashboard(
      long criticalSignals,
      long highSignals,
      long mediumSignals,
      long resolvedThisWeek,
      List<WatchlistEntryWithStudent> watchlistStudents) {}

  public Dashboard dashboard(UUID schoolId) {
    List<UUID> studentIds = schoolStudentIds(schoolId);
    long critical = countBySeverity(studentIds, SignalSeverity.CRITICAL);
    long high = countBySeverity(studentIds, SignalSeverity.HIGH);
    long medium = countBySeverity(studentIds, SignalSeverity.MEDIUM);
    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    long resolvedThisWeek =
        signalRepository.findByStudentIdInOrderByDetectedAtDesc(studentIds).stream()
            .map(PsychologicalSignalEntity::toDomain)
            .filter(PsychologicalSignal::resolved)
            .filter(s -> s.resolvedAt() != null && s.resolvedAt().isAfter(weekAgo))
            .count();
    return new Dashboard(critical, high, medium, resolvedThisWeek, watchlist(schoolId));
  }

  private long countBySeverity(List<UUID> studentIds, SignalSeverity severity) {
    if (studentIds.isEmpty()) {
      return 0;
    }
    return signalRepository
        .findByStudentIdInAndSeverityOrderByDetectedAtDesc(studentIds, severity)
        .stream()
        .filter(e -> !e.isResolved())
        .count();
  }

  public record SignalWithStudent(
      PsychologicalSignal signal, String studentName, String className) {}

  public List<SignalWithStudent> listSignals(
      UUID schoolId, SignalSeverity severity, Boolean resolved) {
    List<UUID> studentIds = schoolStudentIds(schoolId);
    if (studentIds.isEmpty()) {
      return List.of();
    }
    List<PsychologicalSignalEntity> entities =
        severity == null
            ? signalRepository.findByStudentIdInOrderByDetectedAtDesc(studentIds)
            : signalRepository.findByStudentIdInAndSeverityOrderByDetectedAtDesc(
                studentIds, severity);
    return entities.stream()
        .filter(e -> resolved == null || e.isResolved() == resolved)
        .map(this::toSignalWithStudent)
        .toList();
  }

  public record BehaviorProfile(
      Map<String, Integer> activeHours,
      List<XpHistoryEntry> xpTrend,
      List<SubmissionDay> submissionPattern,
      List<String> keyPhrases) {}

  public record SubmissionDay(LocalDate date, int count) {}

  public record SignalDetail(SignalWithStudent signal, BehaviorProfile behaviorProfile) {}

  public SignalDetail getSignalDetail(UUID schoolId, UUID signalId) {
    PsychologicalSignalEntity entity = requireSignal(schoolId, signalId);
    SignalWithStudent signalWithStudent = toSignalWithStudent(entity);
    return new SignalDetail(signalWithStudent, buildBehaviorProfile(entity.getStudentId()));
  }

  private BehaviorProfile buildBehaviorProfile(UUID studentId) {
    LocalDateTime since = LocalDateTime.now().minusDays(TREND_DAYS);
    List<LocalDateTime> submissionTimes = new ArrayList<>();
    homeworkSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
        .map(s -> s.toDomain().submittedAt())
        .filter(t -> t.isAfter(since))
        .forEach(submissionTimes::add);
    examSubmissionRepository.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
        .map(s -> s.toDomain().uploadedAt())
        .filter(t -> t.isAfter(since))
        .forEach(submissionTimes::add);

    Map<String, Integer> activeHours = new LinkedHashMap<>();
    for (int h = 0; h < 24; h += 2) {
      activeHours.put(h + "-" + (h + 2), 0);
    }
    for (LocalDateTime time : submissionTimes) {
      int hour = time.getHour();
      int bucketStart = (hour / 2) * 2;
      String key = bucketStart + "-" + (bucketStart + 2);
      activeHours.merge(key, 1, Integer::sum);
    }

    Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
    for (LocalDateTime time : submissionTimes) {
      byDay.merge(time.toLocalDate(), 1, Integer::sum);
    }
    List<SubmissionDay> submissionPattern =
        byDay.entrySet().stream().map(e -> new SubmissionDay(e.getKey(), e.getValue())).toList();

    List<XpHistoryEntry> xpTrend =
        xpHistoryRepository.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
            .map(x -> x.toDomain())
            .filter(x -> x.occurredAt().isAfter(since))
            .toList();

    // Key phrases intentionally remain empty: a psychologist dashboard must not expose raw,
    // unreviewed student chat content merely because Wellbeing analysis can process it.
    return new BehaviorProfile(activeHours, xpTrend, submissionPattern, List.of());
  }

  public PsychologicalSignal resolve(
      UUID schoolId, UUID signalId, String notes, String actionTaken) {
    PsychologicalSignalEntity entity = requireSignal(schoolId, signalId);
    PsychologicalSignal domain = entity.toDomain();
    PsychologicalSignal resolved =
        new PsychologicalSignal(
            domain.id(),
            domain.studentId(),
            domain.type(),
            domain.severity(),
            domain.description(),
            domain.rawEvidence(),
            domain.isManipulation(),
            domain.notifiedClassTeacher(),
            domain.notifiedParent(),
            domain.notifiedPsychologist(),
            true,
            domain.detectedAt(),
            LocalDateTime.now(),
            notes,
            actionTaken);
    return signalRepository.save(PsychologicalSignalEntity.fromDomain(resolved)).toDomain();
  }

  public PsychologicalSignal markManipulation(UUID schoolId, UUID signalId) {
    PsychologicalSignalEntity entity = requireSignal(schoolId, signalId);
    PsychologicalSignal domain = entity.toDomain();
    PsychologicalSignal updated =
        new PsychologicalSignal(
            domain.id(),
            domain.studentId(),
            domain.type(),
            domain.severity(),
            domain.description(),
            domain.rawEvidence(),
            true,
            domain.notifiedClassTeacher(),
            domain.notifiedParent(),
            domain.notifiedPsychologist(),
            domain.resolved(),
            domain.detectedAt(),
            domain.resolvedAt(),
            domain.resolutionNotes(),
            domain.actionTaken());
    return signalRepository.save(PsychologicalSignalEntity.fromDomain(updated)).toDomain();
  }

  public record WatchlistEntryWithStudent(WatchlistEntry entry, String studentName) {}

  public List<WatchlistEntryWithStudent> watchlist(UUID schoolId) {
    List<UUID> studentIds = schoolStudentIds(schoolId);
    return watchlistRepository.findAllByOrderByAddedAtDesc().stream()
        .map(WatchlistEntryEntity::toDomain)
        .filter(entry -> studentIds.contains(entry.studentId()))
        .map(
            entry ->
                new WatchlistEntryWithStudent(
                    entry,
                    userRepository
                        .findById(entry.studentId())
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse("")))
        .toList();
  }

  public WatchlistEntry addToWatchlist(
      UUID schoolId, UUID psychologistId, UUID studentId, String reason) {
    requireStudentInSchool(schoolId, studentId);
    WatchlistEntry entry =
        watchlistRepository
            .findByStudentId(studentId)
            .map(WatchlistEntryEntity::toDomain)
            .map(
                existing ->
                    new WatchlistEntry(
                        existing.id(), studentId, psychologistId, reason, existing.addedAt()))
            .orElse(
                new WatchlistEntry(
                    UUID.randomUUID(), studentId, psychologistId, reason, LocalDateTime.now()));
    return watchlistRepository.save(WatchlistEntryEntity.fromDomain(entry)).toDomain();
  }

  public void removeFromWatchlist(UUID schoolId, UUID studentId) {
    requireStudentInSchool(schoolId, studentId);
    watchlistRepository.deleteByStudentId(studentId);
  }

  public record MonthlyReport(
      long totalSignals,
      Map<String, Long> bySeverity,
      Map<String, Long> byType,
      long resolvedCount,
      long manipulationFlaggedCount) {}

  public MonthlyReport monthlyReport(UUID schoolId) {
    List<UUID> studentIds = schoolStudentIds(schoolId);
    LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
    List<PsychologicalSignal> signals =
        signalRepository.findByStudentIdInOrderByDetectedAtDesc(studentIds).stream()
            .map(PsychologicalSignalEntity::toDomain)
            .filter(s -> s.detectedAt().isAfter(monthAgo))
            .toList();

    Map<String, Long> bySeverity = new LinkedHashMap<>();
    Map<String, Long> byType = new LinkedHashMap<>();
    for (PsychologicalSignal signal : signals) {
      bySeverity.merge(signal.severity().name(), 1L, Long::sum);
      byType.merge(signal.type().name(), 1L, Long::sum);
    }
    long resolvedCount = signals.stream().filter(PsychologicalSignal::resolved).count();
    long manipulationCount = signals.stream().filter(PsychologicalSignal::isManipulation).count();
    return new MonthlyReport(signals.size(), bySeverity, byType, resolvedCount, manipulationCount);
  }

  private SignalWithStudent toSignalWithStudent(PsychologicalSignalEntity entity) {
    PsychologicalSignal signal = entity.toDomain();
    String studentName =
        userRepository
            .findById(signal.studentId())
            .map(u -> u.getFirstName() + " " + u.getLastName())
            .orElse("");
    String className =
        studentProfileRepository
            .findByUserId(signal.studentId())
            .map(StudentProfileEntity::getClassId)
            .flatMap(classRepository::findById)
            .map(SchoolClassEntity::getFullName)
            .orElse("");
    return new SignalWithStudent(signal, studentName, className);
  }

  private List<UUID> schoolStudentIds(UUID schoolId) {
    return studentProfileRepository.findBySchoolId(schoolId).stream()
        .map(StudentProfileEntity::getUserId)
        .toList();
  }

  private void requireStudentInSchool(UUID schoolId, UUID studentId) {
    if (!schoolStudentIds(schoolId).contains(studentId)) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "ERR_STUDENT_NOT_FOUND",
          "O'quvchi topilmadi.",
          "ID ni tekshiring.");
    }
  }

  private PsychologicalSignalEntity requireSignal(UUID schoolId, UUID signalId) {
    return signalRepository
        .findByIdAndStudentIdIn(signalId, schoolStudentIds(schoolId))
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_SIGNAL_NOT_FOUND",
                    "Signal topilmadi.",
                    "ID ni tekshiring."));
  }
}
