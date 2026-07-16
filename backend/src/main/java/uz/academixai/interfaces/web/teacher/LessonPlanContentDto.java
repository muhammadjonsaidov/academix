package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.domain.LessonPlanContent;

public record LessonPlanContentDto(
    List<String> objectives,
    List<LessonActivityDto> activities,
    List<String> materials,
    String homeworkSuggestion) {

  public record LessonActivityDto(String description, int durationMinutes) {}

  public static LessonPlanContentDto from(LessonPlanContent content) {
    if (content == null) {
      return null;
    }
    return new LessonPlanContentDto(
        content.objectives(),
        content.activities().stream()
            .map(a -> new LessonActivityDto(a.description(), a.durationMinutes()))
            .toList(),
        content.materials(),
        content.homeworkSuggestion());
  }
}
