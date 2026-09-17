package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.LessonPlan;

/** Outbound port for generated lesson plans. */
public interface LessonPlanStore {

  LessonPlan save(LessonPlan plan);

  Optional<LessonPlan> findOwned(UUID teacherId, UUID planId);

  /**
   * A teacher's plans, optionally narrowed by subject and/or class. The four query shapes the
   * legacy repository exposed collapse into one call here — picking between them is storage's
   * problem, not the use case's.
   */
  List<LessonPlan> list(UUID teacherId, UUID subjectId, UUID classId);
}
