package uz.academixai.interfaces.web.teacher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.learning.application.port.in.ExamSubmissionWorkflow;

/** academix_tz.md §2.3 "Nazorat ishi" — exact contract, don't drift path/shape from spec. */
@RestController
@RequestMapping("/api/v1/teacher/exams/{examId}/submissions")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExamSubmissionController {

  private final ExamSubmissionWorkflow examSubmissionService;

  public TeacherExamSubmissionController(ExamSubmissionWorkflow examSubmissionService) {
    this.examSubmissionService = examSubmissionService;
  }

  @PostMapping("/bulk-upload")
  public ResponseEntity<BulkUploadExamSubmissionsResponse> bulkUpload(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID examId,
      @RequestParam("images") List<MultipartFile> images,
      @RequestParam("studentIds") List<UUID> studentIds) {
    var result =
        examSubmissionService.bulkUpload(
            principal.schoolId(), principal.userId(), examId, toImages(images), studentIds);
    return ResponseEntity.ok(new BulkUploadExamSubmissionsResponse(result));
  }

  @GetMapping
  public List<ExamSubmissionResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID examId) {
    return examSubmissionService.list(principal.schoolId(), principal.userId(), examId).stream()
        .map(ExamSubmissionResponse::from)
        .toList();
  }

  @PostMapping("/{id}/grade")
  public ResponseEntity<Void> grade(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID examId,
      @PathVariable UUID id,
      @RequestBody GradeExamSubmissionRequest request) {
    examSubmissionService.grade(
        principal.schoolId(),
        principal.userId(),
        examId,
        id,
        request.score(),
        request.fivePointGrade(),
        request.teacherComment());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/approve-all")
  public ResponseEntity<Void> approveAll(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID examId) {
    examSubmissionService.approveAll(principal.schoolId(), principal.userId(), examId);
    return ResponseEntity.noContent().build();
  }

  private static List<ExamSubmissionWorkflow.Image> toImages(List<MultipartFile> images) {
    return images.stream().map(TeacherExamSubmissionController::toImage).toList();
  }

  private static ExamSubmissionWorkflow.Image toImage(MultipartFile image) {
    try {
      return new ExamSubmissionWorkflow.Image(
          image == null ? null : image.getBytes(),
          image == null ? null : image.getContentType(),
          image == null ? null : image.getOriginalFilename());
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not read exam upload", exception);
    }
  }
}
