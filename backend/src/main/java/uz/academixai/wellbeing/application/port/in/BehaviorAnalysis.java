package uz.academixai.wellbeing.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.PsychologicalSignal;

/** Published Wellbeing command for the nightly student-behavior analysis. */
public interface BehaviorAnalysis {

  List<PsychologicalSignal> analyzeStudent(UUID studentId);

  int analyzeAllActiveStudents();
}
