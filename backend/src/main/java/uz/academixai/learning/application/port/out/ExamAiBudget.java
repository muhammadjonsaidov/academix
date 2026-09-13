package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Learning's narrow view of the AI budget policy. */
public interface ExamAiBudget {

  int remainingCalls(UUID schoolId);
}
