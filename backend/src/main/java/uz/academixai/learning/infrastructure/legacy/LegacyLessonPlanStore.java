package uz.academixai.learning.infrastructure.legacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.LessonPlan;
import uz.academixai.infrastructure.persistence.LessonPlanEntity;
import uz.academixai.infrastructure.persistence.LessonPlanRepository;
import uz.academixai.learning.application.port.out.LessonPlanStore;

/** Adapter for {@link LessonPlanStore} over the legacy lesson-plan repository. */
@Component
public class LegacyLessonPlanStore implements LessonPlanStore {

  private final LessonPlanRepository plans;

  public LegacyLessonPlanStore(LessonPlanRepository plans) {
    this.plans = plans;
  }

  @Override
  public LessonPlan save(LessonPlan plan) {
    return plans.save(LessonPlanEntity.fromDomain(plan)).toDomain();
  }

  @Override
  public Optional<LessonPlan> findOwned(UUID teacherId, UUID planId) {
    return plans.findByIdAndTeacherId(planId, teacherId).map(LessonPlanEntity::toDomain);
  }

  @Override
  public List<LessonPlan> list(UUID teacherId, UUID subjectId, UUID classId) {
    var entities =
        switch ((subjectId != null ? 1 : 0) + (classId != null ? 2 : 0)) {
          case 3 ->
              plans.findByTeacherIdAndSubjectIdAndClassIdOrderByLessonDateDesc(
                  teacherId, subjectId, classId);
          case 2 -> plans.findByTeacherIdAndClassIdOrderByLessonDateDesc(teacherId, classId);
          case 1 -> plans.findByTeacherIdAndSubjectIdOrderByLessonDateDesc(teacherId, subjectId);
          default -> plans.findByTeacherIdOrderByLessonDateDesc(teacherId);
        };
    return entities.stream().map(LessonPlanEntity::toDomain).toList();
  }
}
