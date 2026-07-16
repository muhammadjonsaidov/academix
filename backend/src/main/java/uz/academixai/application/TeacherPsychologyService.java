package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Psixologik signallar (faqat sinf rahbari ko'radi)" — homeroom-teacher-only,
 * scoped via {@code school_classes.class_teacher_id}, same ownership pattern as {@code
 * HandwritingService.requireClassTeacher}. Response shapes aren't spelled out in the spec (only
 * paths are given) — inferred from the psychologist API's shape minus the behavior-profile/
 * manipulation fields, which are psychologist-only per TZ §7.1 ("hech qanday dars mazmuni yoki
 * baholar ko'rinmaydi" doesn't apply here, but the manipulation flag stays psychologist-facing per
 * §1.14 "faqat psixologga alohida ko'rinadi").
 */
@Service
public class TeacherPsychologyService {

  private final SchoolClassRepository classRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final PsychologicalSignalRepository signalRepository;
  private final UserRepository userRepository;

  public TeacherPsychologyService(
      SchoolClassRepository classRepository,
      StudentProfileRepository studentProfileRepository,
      PsychologicalSignalRepository signalRepository,
      UserRepository userRepository) {
    this.classRepository = classRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.signalRepository = signalRepository;
    this.userRepository = userRepository;
  }

  public record SignalWithStudent(PsychologicalSignal signal, String studentName) {}

  public List<SignalWithStudent> list(UUID schoolId, UUID teacherId, SignalSeverity severity) {
    List<UUID> studentIds = myStudentIds(schoolId, teacherId);
    if (studentIds.isEmpty()) {
      return List.of();
    }
    List<PsychologicalSignalEntity> entities =
        severity == null
            ? signalRepository.findByStudentIdInOrderByDetectedAtDesc(studentIds)
            : signalRepository.findByStudentIdInAndSeverityOrderByDetectedAtDesc(
                studentIds, severity);
    return entities.stream().map(this::toSignalWithStudent).toList();
  }

  public SignalWithStudent get(UUID schoolId, UUID teacherId, UUID signalId) {
    List<UUID> studentIds = myStudentIds(schoolId, teacherId);
    PsychologicalSignalEntity entity =
        signalRepository
            .findByIdAndStudentIdIn(signalId, studentIds)
            .orElseThrow(TeacherPsychologyService::notFound);
    return toSignalWithStudent(entity);
  }

  public PsychologicalSignal resolve(UUID schoolId, UUID teacherId, UUID signalId) {
    List<UUID> studentIds = myStudentIds(schoolId, teacherId);
    PsychologicalSignalEntity entity =
        signalRepository
            .findByIdAndStudentIdIn(signalId, studentIds)
            .orElseThrow(TeacherPsychologyService::notFound);
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
            domain.resolutionNotes(),
            domain.actionTaken());
    return signalRepository.save(PsychologicalSignalEntity.fromDomain(resolved)).toDomain();
  }

  private SignalWithStudent toSignalWithStudent(PsychologicalSignalEntity entity) {
    PsychologicalSignal signal = entity.toDomain();
    String studentName =
        userRepository
            .findById(signal.studentId())
            .map(u -> u.getFirstName() + " " + u.getLastName())
            .orElse("");
    return new SignalWithStudent(signal, studentName);
  }

  private List<UUID> myStudentIds(UUID schoolId, UUID teacherId) {
    List<SchoolClassEntity> myClasses =
        classRepository.findBySchoolIdAndClassTeacherId(schoolId, teacherId);
    return myClasses.stream()
        .flatMap(
            schoolClass ->
                studentProfileRepository
                    .findByClassIdAndSchoolId(schoolClass.getId(), schoolId)
                    .stream())
        .map(StudentProfileEntity::getUserId)
        .toList();
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_SIGNAL_NOT_FOUND",
        "Signal topilmadi.",
        "ID ni tekshiring yoki sinf rahbarligingizni tasdiqlang.");
  }
}
