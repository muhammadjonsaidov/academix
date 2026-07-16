package uz.academixai.interfaces.web.student;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.StudentExamService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * Gap-fill deviation, judgment call (see StudentExamService's Javadoc): no student-facing exam
 * results endpoint exists anywhere in academix_tz.md — this mirrors {@code
 * StudentHomeworkController}'s submissions shape.
 */
@RestController
@RequestMapping("/api/v1/student/exams")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

  private final StudentExamService examService;

  public StudentExamController(StudentExamService examService) {
    this.examService = examService;
  }

  @GetMapping
  public List<StudentExamListItemResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return examService.list(principal.schoolId(), principal.userId()).stream()
        .map(StudentExamListItemResponse::from)
        .toList();
  }

  @GetMapping("/{examId}")
  public StudentExamDetailResponse get(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID examId) {
    return StudentExamDetailResponse.from(
        examService.getDetail(principal.schoolId(), principal.userId(), examId));
  }
}
