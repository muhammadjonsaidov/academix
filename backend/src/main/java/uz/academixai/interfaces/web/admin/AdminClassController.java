package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.school.application.ClassAdministrationService;

/** academix_tz.md §2.2 "Sinflar" — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/admin/classes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClassController {

  private final ClassAdministrationService classService;

  public AdminClassController(ClassAdministrationService classService) {
    this.classService = classService;
  }

  @GetMapping
  public List<ClassResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return classService.list(principal.schoolId()).stream().map(ClassResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<ClassResponse> create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateClassRequest request) {
    var created =
        classService.create(
            principal.schoolId(), request.grade(), request.letter(), request.classTeacherId());
    return ResponseEntity.ok(ClassResponse.from(created));
  }

  @PutMapping("/{classId}")
  public ClassResponse update(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID classId,
      @RequestBody CreateClassRequest request) {
    var updated =
        classService.update(
            principal.schoolId(),
            classId,
            request.grade(),
            request.letter(),
            request.classTeacherId());
    return ClassResponse.from(updated);
  }

  @DeleteMapping("/{classId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID classId) {
    classService.delete(principal.schoolId(), classId);
    return ResponseEntity.noContent().build();
  }
}
