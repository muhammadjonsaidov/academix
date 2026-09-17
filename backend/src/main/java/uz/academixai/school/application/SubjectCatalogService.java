package uz.academixai.school.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Subject;
import uz.academixai.domain.SubjectType;
import uz.academixai.school.application.port.out.SubjectCatalogRepository;
import uz.academixai.shared.error.ApiException;

/** Administrative use cases for one tenant's subject catalog. */
@Service
public class SubjectCatalogService {

  private final SubjectCatalogRepository subjects;

  public SubjectCatalogService(SubjectCatalogRepository subjects) {
    this.subjects = subjects;
  }

  public List<Subject> list(UUID schoolId) {
    return subjects.findBySchoolId(schoolId);
  }

  public Subject create(UUID schoolId, String rawName, String rawType, String icon) {
    String name = requireName(rawName);
    SubjectType type = parseType(rawType);
    if (subjects.existsBySchoolIdAndNameIgnoringCase(schoolId, name)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SUBJECT_EXISTS",
          "Bu nomdagi fan allaqachon mavjud.",
          "Boshqa nom kiriting.");
    }
    return subjects.save(new Subject(UUID.randomUUID(), schoolId, name, type, icon));
  }

  public void delete(UUID schoolId, UUID subjectId) {
    subjects
        .findByIdAndSchoolId(subjectId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_SUBJECT_NOT_FOUND",
                    "Fan topilmadi.",
                    "ID ni tekshiring."));
    // Subject FKs are ON DELETE CASCADE. Refuse deletion while referenced rather than allowing a
    // catalog action to erase assignments, homework, exams or lesson-planning data.
    if (subjects.isReferenced(subjectId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SUBJECT_IN_USE",
          "Bu fan biriktiruvlar yoki topshiriqlarda ishlatilmoqda — o'chirib bo'lmaydi.",
          "Avval fanga tegishli biriktiruvlarni olib tashlang.");
    }
    subjects.deleteById(subjectId);
  }

  private static String requireName(String rawName) {
    if (rawName == null || rawName.isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "ERR_VALIDATION", "Fan nomi majburiy.", "Fan nomini kiriting.");
    }
    return rawName.trim();
  }

  private static SubjectType parseType(String rawType) {
    try {
      return SubjectType.valueOf(rawType);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_SUBJECT",
          "Fan turi noto'g'ri.",
          "Ro'yxatdagi turlardan birini tanlang.");
    }
  }
}
