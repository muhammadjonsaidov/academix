package uz.academixai.interfaces.web.admin;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.school.application.SchoolAdministrationService;

/** academix_tz.md §2.2 "Maktab". */
@RestController
@RequestMapping("/api/v1/admin/school")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchoolController {

  private final SchoolAdministrationService schoolService;

  public AdminSchoolController(SchoolAdministrationService schoolService) {
    this.schoolService = schoolService;
  }

  @GetMapping
  public SchoolResponse get(@AuthenticationPrincipal AcademixPrincipal principal) {
    return SchoolResponse.from(schoolService.get(principal.schoolId()));
  }

  @PutMapping
  public SchoolResponse update(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @Valid @RequestBody UpdateSchoolRequest request) {
    return SchoolResponse.from(
        schoolService.update(
            principal.schoolId(),
            request.name(),
            request.address(),
            request.region(),
            request.district(),
            request.phone()));
  }
}
