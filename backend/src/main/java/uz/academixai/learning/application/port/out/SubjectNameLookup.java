package uz.academixai.learning.application.port.out;

import java.util.UUID;
import uz.academixai.shared.ai.GradingMode;

/** School catalog read needed by the student homework card. */
public interface SubjectNameLookup {

  String name(UUID subjectId);

  /** How work in this subject is checked — decides how the analysis is labelled. */
  GradingMode gradingMode(UUID subjectId);
}
