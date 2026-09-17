package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.identity.application.StudentPasswordResetService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * academix_tz.md §2.1 — class-teacher-assisted STUDENT password reset (students have no email
 * channel, unlike the ADMIN/TEACHER/PARENT/PSYCHOLOGIST forgot-password flow).
 */
@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}")
@PreAuthorize("hasRole('TEACHER')")
public class StudentPasswordResetController {

  private final StudentPasswordResetService resetService;

  public StudentPasswordResetController(StudentPasswordResetService resetService) {
    this.resetService = resetService;
  }

  @PutMapping("/reset-password")
  public StudentPasswordResetResponse resetPassword(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    String tempPassword =
        resetService.resetPassword(principal.schoolId(), studentId, principal.userId());
    return new StudentPasswordResetResponse(tempPassword);
  }
}
