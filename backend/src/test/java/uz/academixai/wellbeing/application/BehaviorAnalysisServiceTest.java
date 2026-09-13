package uz.academixai.wellbeing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;
import uz.academixai.wellbeing.application.port.in.BehaviorAnalysis;
import uz.academixai.wellbeing.application.port.out.ActiveStudentDirectory;
import uz.academixai.wellbeing.application.port.out.BehaviorActivityLookup;
import uz.academixai.wellbeing.application.port.out.BehaviorAnalyzer;
import uz.academixai.wellbeing.application.port.out.EvidencePayloadSerializer;
import uz.academixai.wellbeing.application.port.out.PsychologicalAlertNotifier;
import uz.academixai.wellbeing.application.port.out.PsychologicalSignalStore;
import uz.academixai.wellbeing.domain.SignalCandidate;

class BehaviorAnalysisServiceTest {

  @Test
  void criticalCandidatePersistsSignalAndUsesParentAndStaffAlertPaths() {
    UUID studentId = UUID.randomUUID();
    UUID schoolId = UUID.randomUUID();
    Store store = new Store();
    Alerts alerts = new Alerts();
    BehaviorAnalysis analysis =
        service(
            studentId,
            schoolId,
            store,
            alerts,
            summary ->
                new uz.academixai.wellbeing.domain.BehaviorAnalysis(
                    List.of(new SignalCandidate("NEGATIVE_LANGUAGE", "CRITICAL", "xafa kayfiyat")),
                    false));

    List<PsychologicalSignal> created = analysis.analyzeStudent(studentId);

    assertThat(created)
        .singleElement()
        .satisfies(
            signal -> {
              assertThat(signal.severity()).isEqualTo(SignalSeverity.CRITICAL);
              assertThat(signal.notifiedParent()).isTrue();
              assertThat(signal.notifiedClassTeacher()).isTrue();
              assertThat(signal.rawEvidence()).isEqualTo("json:xafa kayfiyat");
            });
    assertThat(alerts.parentNotifications).isEqualTo(1);
    assertThat(alerts.staffNotifications).isEqualTo(1);
  }

  @Test
  void malformedProviderCandidateIsIgnored() {
    UUID studentId = UUID.randomUUID();
    Store store = new Store();
    BehaviorAnalysis analysis =
        service(
            studentId,
            UUID.randomUUID(),
            store,
            new Alerts(),
            summary ->
                new uz.academixai.wellbeing.domain.BehaviorAnalysis(
                    List.of(new SignalCandidate("NOT_A_SIGNAL", "HIGH", "ignored")), false));

    assertThat(analysis.analyzeStudent(studentId)).isEmpty();
    assertThat(store.saved).isEmpty();
  }

  private static BehaviorAnalysis service(
      UUID studentId, UUID schoolId, Store store, Alerts alerts, BehaviorAnalyzer analyzer) {
    ActiveStudentDirectory students =
        new ActiveStudentDirectory() {
          @Override
          public List<Student> findAllActive() {
            return List.of(new Student(studentId, schoolId));
          }

          @Override
          public Optional<UUID> findSchoolId(UUID requestedStudentId) {
            return studentId.equals(requestedStudentId) ? Optional.of(schoolId) : Optional.empty();
          }
        };
    BehaviorActivityLookup activity =
        (requestedSchoolId, requestedStudentId, since, maxMessages) ->
            new BehaviorActivityLookup.Activity(
                List.of(LocalDateTime.now().minusHours(1)),
                10,
                List.of(),
                LocalDate.now(),
                List.of("yordam kerak"));
    EvidencePayloadSerializer evidence = value -> "json:" + value;
    return new BehaviorAnalysisService(students, activity, analyzer, store, alerts, evidence);
  }

  private static final class Store implements PsychologicalSignalStore {

    private final List<PsychologicalSignal> saved = new ArrayList<>();

    @Override
    public PsychologicalSignal save(PsychologicalSignal signal) {
      saved.add(signal);
      return signal;
    }
  }

  private static final class Alerts implements PsychologicalAlertNotifier {

    private int parentNotifications;
    private int staffNotifications;

    @Override
    public boolean notifyParents(UUID studentId, SignalType type, SignalSeverity severity) {
      parentNotifications++;
      return true;
    }

    @Override
    public void notifyTeacherAndPsychologists(
        UUID studentId, SignalType type, SignalSeverity severity) {
      staffNotifications++;
    }
  }
}
