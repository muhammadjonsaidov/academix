package uz.academixai.interfaces.web.student;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.application.StudentSubmissionService;
import uz.academixai.domain.SubmissionType;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.4 "Topshirish" — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/student/homework")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSubmissionController {

  private final StudentSubmissionService submissionService;

  public StudentSubmissionController(StudentSubmissionService submissionService) {
    this.submissionService = submissionService;
  }

  @PostMapping("/{assignmentId}/submit")
  public ResponseEntity<SubmitHomeworkResponse> submit(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID assignmentId,
      @RequestParam SubmissionType type,
      @RequestParam(required = false) String textContent,
      @RequestParam(required = false) MultipartFile image) {
    var submission =
        submissionService.submit(
            principal.schoolId(), principal.userId(), assignmentId, type, textContent, image);
    return ResponseEntity.ok(SubmitHomeworkResponse.from(submission));
  }
}
