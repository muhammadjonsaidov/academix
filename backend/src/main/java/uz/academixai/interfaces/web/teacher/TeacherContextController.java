package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.TeacherContextService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3 "Mening sinflarim va fanlarim" — exact contract, don't drift. */
@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherContextController {

  private final TeacherContextService teacherContextService;

  public TeacherContextController(TeacherContextService teacherContextService) {
    this.teacherContextService = teacherContextService;
  }

  @GetMapping("/classes")
  public List<TeacherClassResponse> classes(@AuthenticationPrincipal AcademixPrincipal principal) {
    return teacherContextService.myClasses(principal.schoolId(), principal.userId()).stream()
        .map(TeacherClassResponse::from)
        .toList();
  }

  @GetMapping("/subjects")
  public List<TeacherSubjectResponse> subjects(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return teacherContextService.mySubjects(principal.schoolId(), principal.userId()).stream()
        .map(TeacherSubjectResponse::from)
        .toList();
  }

  @GetMapping("/classes/{classId}/students")
  public List<TeacherStudentResponse> classStudents(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID classId) {
    return teacherContextService
        .classStudents(principal.schoolId(), principal.userId(), classId)
        .stream()
        .map(TeacherStudentResponse::from)
        .toList();
  }
}
