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
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.TeacherManagementService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.2 "O'qituvchilar" — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/admin/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherController {

  private final TeacherManagementService teacherService;

  public AdminTeacherController(TeacherManagementService teacherService) {
    this.teacherService = teacherService;
  }

  @GetMapping
  public List<TeacherResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return teacherService.list(principal.schoolId()).stream().map(TeacherResponse::from).toList();
  }

  @PostMapping("/invite")
  public ResponseEntity<TeacherResponse> invite(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody InviteTeacherRequest request) {
    var invited =
        teacherService.invite(
            principal.schoolId(),
            request.phone(),
            request.firstName(),
            request.lastName(),
            request.email());
    return ResponseEntity.ok(TeacherResponse.from(invited));
  }

  @PutMapping("/{teacherId}/activate")
  public TeacherResponse activate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID teacherId) {
    return TeacherResponse.from(teacherService.activate(principal.schoolId(), teacherId));
  }

  @PutMapping("/{teacherId}/deactivate")
  public TeacherResponse deactivate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID teacherId) {
    return TeacherResponse.from(teacherService.deactivate(principal.schoolId(), teacherId));
  }
}
