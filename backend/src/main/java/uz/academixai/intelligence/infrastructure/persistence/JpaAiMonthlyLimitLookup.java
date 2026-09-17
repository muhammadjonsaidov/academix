package uz.academixai.intelligence.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.intelligence.application.port.out.AiMonthlyLimitLookup;

/** JPA adapter for the School-owned monthly AI allowance. */
@Repository
public class JpaAiMonthlyLimitLookup implements AiMonthlyLimitLookup {

  private final SchoolRepository schools;

  public JpaAiMonthlyLimitLookup(SchoolRepository schools) {
    this.schools = schools;
  }

  @Override
  public Optional<Integer> monthlyLimit(UUID schoolId) {
    return schools.findById(schoolId).map(school -> school.getMonthlyAiCallLimit());
  }
}
