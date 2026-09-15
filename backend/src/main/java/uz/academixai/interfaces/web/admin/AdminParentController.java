package uz.academixai.interfaces.web.admin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.family.application.ParentManagementService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * Admin parent lifecycle — create/list/check. DEVIATION beyond academix_tz.md §2.2 (which only
 * documents {@code POST /admin/parents/link}) — see {@link ParentManagementService}'s Javadoc for
 * the full rationale. The link endpoint itself stays in {@link AdminParentLinkController}.
 */
@RestController
@RequestMapping("/api/v1/admin/parents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminParentController {

  private final ParentManagementService parentManagementService;

  public AdminParentController(ParentManagementService parentManagementService) {
    this.parentManagementService = parentManagementService;
  }

  @PostMapping
  public ParentResponse create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateParentRequest request) {
    var parent =
        parentManagementService.create(
            principal.schoolId(),
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.email(),
            request.password());
    return ParentResponse.from(parent, List.of());
  }

  @GetMapping
  public List<ParentResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return parentManagementService.list(principal.schoolId()).stream()
        .map(p -> ParentResponse.from(p.parent(), p.children()))
        .toList();
  }

  @GetMapping("/check")
  public ParentCheckResponse check(@RequestParam("phone") String phone) {
    return ParentCheckResponse.from(parentManagementService.check(phone));
  }
}
