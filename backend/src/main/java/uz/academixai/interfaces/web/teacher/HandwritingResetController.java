package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.HandwritingService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.2/§1.13/§6.3 "Yozuv profilini reset qilish" — exact contract. */
@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/handwriting")
@PreAuthorize("hasRole('TEACHER')")
public class HandwritingResetController {

  private final HandwritingService handwritingService;

  public HandwritingResetController(HandwritingService handwritingService) {
    this.handwritingService = handwritingService;
  }

  @PutMapping("/reset")
  public HandwritingResetResponse reset(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID studentId,
      @RequestBody HandwritingResetRequest request) {
    var result =
        handwritingService.resetProfile(
            principal.schoolId(), studentId, principal.userId(), request.reason(), request.notes());
    return HandwritingResetResponse.from(result);
  }
}
