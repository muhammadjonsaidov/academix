package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.learning.application.port.in.ExamManagement;

/**
 * academix_tz.md §2.3 "Nazorat ishi (imtihon)" — exact contract for create, don't drift path/shape
 * from spec. {@code GET} (list) is a gap-fill deviation, see {@link ExamListItemResponse}.
 */
@RestController
@RequestMapping("/api/v1/teacher/exams")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExamController {

  private final ExamManagement examService;

  public TeacherExamController(ExamManagement examService) {
    this.examService = examService;
  }

  @PostMapping
  public ResponseEntity<CreateExamResponse> create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateExamRequest request) {
    var result =
        examService.create(
            principal.schoolId(),
            principal.userId(),
            request.classId(),
            request.subjectId(),
            request.title(),
            request.examDate(),
            request.maxScore());
    return ResponseEntity.ok(
        new CreateExamResponse(
            result.exam().id(),
            result.estimatedAiCalls(),
            result.remainingExamBudget(),
            result.warning()));
  }

  @GetMapping
  public List<ExamListItemResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) UUID subjectId) {
    return examService
        .listWithCounts(principal.schoolId(), principal.userId(), classId, subjectId)
        .stream()
        .map(ExamListItemResponse::from)
        .toList();
  }
}
