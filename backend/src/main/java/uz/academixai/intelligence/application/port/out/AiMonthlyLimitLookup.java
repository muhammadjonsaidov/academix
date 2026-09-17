package uz.academixai.intelligence.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** School configuration boundary for the monthly AI-call allowance. */
public interface AiMonthlyLimitLookup {

  Optional<Integer> monthlyLimit(UUID schoolId);
}
