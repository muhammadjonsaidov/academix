package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.LessonPlanService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3 "Dars rejasi" — exact contract, don't drift path/shape from spec. */
@RestController
@RequestMapping("/api/v1/teacher/lesson-plans")
@PreAuthorize("hasRole('TEACHER')")
public class LessonPlanController {

  private final LessonPlanService lessonPlanService;

  public LessonPlanController(LessonPlanService lessonPlanService) {
    this.lessonPlanService = lessonPlanService;
  }

  @PostMapping("/generate")
  public LessonPlanResponse generate(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody GenerateLessonPlanRequest request) {
    var plan =
        lessonPlanService.generate(
            principal.userId(),
            request.syllabusId(),
            request.topic(),
            request.lessonDate(),
            request.classId());
    return LessonPlanResponse.from(plan);
  }

  @GetMapping
  public List<LessonPlanResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) UUID subjectId) {
    return lessonPlanService.list(principal.userId(), subjectId, classId).stream()
        .map(LessonPlanResponse::from)
        .toList();
  }

  @PutMapping("/{planId}")
  public LessonPlanResponse update(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID planId,
      @RequestBody UpdateLessonPlanRequest request) {
    var plan =
        lessonPlanService.update(
            principal.userId(), planId, request.teacherEditedPlan(), request.isApproved());
    return LessonPlanResponse.from(plan);
  }
}
