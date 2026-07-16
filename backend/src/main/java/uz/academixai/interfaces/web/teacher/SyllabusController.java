package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.application.SyllabusService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3 "Darslik yuklash" — exact contract, don't drift path/shape from spec. */
@RestController
@RequestMapping("/api/v1/teacher/syllabuses")
@PreAuthorize("hasRole('TEACHER')")
public class SyllabusController {

  private final SyllabusService syllabusService;

  public SyllabusController(SyllabusService syllabusService) {
    this.syllabusService = syllabusService;
  }

  @PostMapping
  public SyllabusResponse upload(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestPart("file") MultipartFile file,
      @RequestPart("subjectId") String subjectId,
      @RequestPart("classId") String classId,
      @RequestPart("title") String title) {
    var syllabus =
        syllabusService.upload(
            principal.userId(), UUID.fromString(subjectId), UUID.fromString(classId), title, file);
    return SyllabusResponse.from(syllabus);
  }

  @GetMapping
  public List<SyllabusResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) UUID classId) {
    return syllabusService.list(principal.userId(), subjectId, classId).stream()
        .map(SyllabusResponse::from)
        .toList();
  }

  @GetMapping("/{syllabusId}")
  public SyllabusResponse get(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID syllabusId) {
    return SyllabusResponse.from(syllabusService.get(principal.userId(), syllabusId));
  }
}
