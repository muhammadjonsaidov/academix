package uz.academixai.intelligence.application.port.out;

import java.util.UUID;
import uz.academixai.intelligence.domain.AiCallCategory;

/** Tells a school's administrators that one of its monthly AI sub-budgets is running out. */
public interface AiBudgetAlerts {

  void budgetRunningLow(UUID schoolId, AiCallCategory category, long used, int allocated);
}
