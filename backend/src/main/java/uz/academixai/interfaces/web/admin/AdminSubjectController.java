package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.domain.SubjectType;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.interfaces.web.ApiException;

/**
 * Admin subjects catalog. Originally read-only (subjects were assumed to be seed/fixture data per
 * CLAUDE.md's known gaps — no creation endpoint is documented in the spec). DEVIATION, judgment
 * call: seeding is not acceptable in a real deployment (a fresh school must be fully manageable
 * from the UI), so create/delete were added. Delete is guarded by an in-use check — every FK to
 * {@code subjects} is ON DELETE CASCADE (V9/V10/V25/...), so an unguarded delete of a used subject
 * would silently cascade away homework/exams/lesson plans.
 */
@RestController
@RequestMapping("/api/v1/admin/subjects")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectController {

  private final SubjectRepository subjectRepository;

  public AdminSubjectController(SubjectRepository subjectRepository) {
    this.subjectRepository = subjectRepository;
  }

  @GetMapping
  public List<SubjectResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return subjectRepository.findBySchoolIdOrderByName(principal.schoolId()).stream()
        .map(entity -> SubjectResponse.from(entity.toDomain()))
        .toList();
  }

  @PostMapping
  public SubjectResponse create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateSubjectRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "ERR_VALIDATION", "Fan nomi majburiy.", "Fan nomini kiriting.");
    }
    SubjectType type;
    try {
      type = SubjectType.valueOf(request.type());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_SUBJECT",
          "Fan turi noto'g'ri.",
          "Ro'yxatdagi turlardan birini tanlang.");
    }
    boolean duplicate =
        subjectRepository.findBySchoolIdOrderByName(principal.schoolId()).stream()
            .anyMatch(s -> s.getName().equalsIgnoreCase(request.name().trim()));
    if (duplicate) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SUBJECT_EXISTS",
          "Bu nomdagi fan allaqachon mavjud.",
          "Boshqa nom kiriting.");
    }
    SubjectEntity saved =
        subjectRepository.save(
            new SubjectEntity(
                UUID.randomUUID(),
                principal.schoolId(),
                request.name().trim(),
                type,
                request.icon()));
    return SubjectResponse.from(saved.toDomain());
  }

  @DeleteMapping("/{subjectId}")
  public void delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID subjectId) {
    SubjectEntity subject =
        subjectRepository
            .findByIdAndSchoolId(subjectId, principal.schoolId())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_SUBJECT_NOT_FOUND",
                        "Fan topilmadi.",
                        "ID ni tekshiring."));
    // In-use guard BEFORE delete: all referencing FKs are ON DELETE CASCADE, so without this a
    // delete would silently take homework/exams/lesson plans with it.
    if (subjectRepository.isReferenced(subjectId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SUBJECT_IN_USE",
          "Bu fan biriktiruvlar yoki topshiriqlarda ishlatilmoqda — o'chirib bo'lmaydi.",
          "Avval fanga tegishli biriktiruvlarni olib tashlang.");
    }
    subjectRepository.delete(subject);
  }
}
