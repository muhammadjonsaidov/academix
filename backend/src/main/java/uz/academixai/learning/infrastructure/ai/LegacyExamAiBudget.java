package uz.academixai.learning.infrastructure.ai;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.AiBudgetService;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.learning.application.port.out.ExamAiBudget;

/** Explicit adapter until the Intelligence budget policy becomes a published API. */
@Component
public class LegacyExamAiBudget implements ExamAiBudget {

  private final AiBudgetService budgetService;

  public LegacyExamAiBudget(AiBudgetService budgetService) {
    this.budgetService = budgetService;
  }

  @Override
  public int remainingCalls(UUID schoolId) {
    return budgetService.remainingBudget(schoolId, AiCallCategory.EXAM);
  }
}
