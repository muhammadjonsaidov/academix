package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
import uz.academixai.application.HandwritingService;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.interfaces.web.PageResponse;
import uz.academixai.school.application.StudentManagementService;

/** academix_tz.md §2.2 "O'quvchilar" — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/admin/students")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {

  private final StudentManagementService studentService;
  private final HandwritingService handwritingService;

  public AdminStudentController(
      StudentManagementService studentService, HandwritingService handwritingService) {
    this.studentService = studentService;
    this.handwritingService = handwritingService;
  }

  @GetMapping
  public PageResponse<StudentResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<StudentResponse> all =
        studentService.list(principal.schoolId(), classId, search).stream()
            .map(StudentResponse::from)
            .toList();
    return PageResponse.slice(all, page, size);
  }

  @PostMapping
  public ResponseEntity<StudentResponse> create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateStudentRequest request) {
    StudentProfile created =
        studentService.create(
            principal.schoolId(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.email(),
            request.password(),
            request.classId(),
            request.studentNumber(),
            request.birthDate());
    var response =
        new StudentResponse(
            created.userId(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            created.classId(),
            created.studentNumber(),
            created.birthDate(),
            created.isActive());
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{studentId}/transfer-class")
  public ResponseEntity<Void> transferClass(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID studentId,
      @RequestBody TransferClassRequest request) {
    studentService.transferClass(principal.schoolId(), studentId, request.newClassId());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{studentId}/handwriting/unlock-reset")
  public ResponseEntity<Void> unlockHandwritingReset(@PathVariable UUID studentId) {
    handwritingService.unlockReset(studentId);
    return ResponseEntity.noContent().build();
  }
}
