package uz.academixai.wellbeing.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.shared.error.ApiException;
import uz.academixai.wellbeing.application.port.out.PsychologicalSignalStore;
import uz.academixai.wellbeing.application.port.out.StudentPresentationLookup;
import uz.academixai.wellbeing.application.port.out.TeacherClassRoster;
import uz.academixai.wellbeing.application.port.out.TeacherSignalStore;

/**
 * academix_tz.md §2.3 — the class teacher's view of psychological signals for their own class.
 *
 * <p>Moved here from the legacy {@code application} package. Every read is bounded by the teacher's
 * roster, and that boundary now lives in {@link TeacherSignalStore} rather than in each call site:
 * a query that takes the student list cannot accidentally be widened to the whole school, which is
 * what the school-scoped psychologist port would have allowed.
 *
 * <p>Three of the five dependencies the move needed already existed — {@code
 * PsychologicalSignalStore} for saving and {@code StudentPresentationLookup} for the student name,
 * which computes exactly the "first last" the inline version did.
 */
@Service
public class TeacherPsychologyService {

  private final TeacherClassRoster roster;
  private final TeacherSignalStore signals;
  private final StudentPresentationLookup students;
  private final PsychologicalSignalStore signalStore;

  public TeacherPsychologyService(
      TeacherClassRoster roster,
      TeacherSignalStore signals,
      StudentPresentationLookup students,
      PsychologicalSignalStore signalStore) {
    this.roster = roster;
    this.signals = signals;
    this.students = students;
    this.signalStore = signalStore;
  }

  public record SignalWithStudent(PsychologicalSignal signal, String studentName) {}

  public List<SignalWithStudent> list(UUID schoolId, UUID teacherId, SignalSeverity severity) {
    List<UUID> studentIds = roster.studentIdsOf(schoolId, teacherId);
    if (studentIds.isEmpty()) {
      return List.of();
    }
    return signals.signalsOf(studentIds, severity).stream().map(this::toSignalWithStudent).toList();
  }

  public SignalWithStudent get(UUID schoolId, UUID teacherId, UUID signalId) {
    List<UUID> studentIds = roster.studentIdsOf(schoolId, teacherId);
    return signals
        .signalOf(studentIds, signalId)
        .map(this::toSignalWithStudent)
        .orElseThrow(TeacherPsychologyService::notFound);
  }

  public PsychologicalSignal resolve(UUID schoolId, UUID teacherId, UUID signalId) {
    List<UUID> studentIds = roster.studentIdsOf(schoolId, teacherId);
    PsychologicalSignal signal =
        signals.signalOf(studentIds, signalId).orElseThrow(TeacherPsychologyService::notFound);
    return signalStore.save(
        new PsychologicalSignal(
            signal.id(),
            signal.studentId(),
            signal.type(),
            signal.severity(),
            signal.description(),
            signal.rawEvidence(),
            signal.isManipulation(),
            signal.notifiedClassTeacher(),
            signal.notifiedParent(),
            signal.notifiedPsychologist(),
            true,
            signal.detectedAt(),
            LocalDateTime.now(),
            signal.resolutionNotes(),
            signal.actionTaken()));
  }

  private SignalWithStudent toSignalWithStudent(PsychologicalSignal signal) {
    String studentName = students.find(signal.studentId()).map(s -> s.fullName()).orElse("");
    return new SignalWithStudent(signal, studentName);
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_SIGNAL_NOT_FOUND",
        "Signal topilmadi.",
        "ID ni tekshiring yoki sinf rahbarligingizni tasdiqlang.");
  }
}
