package uz.academixai.application;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.interfaces.web.ApiException;

/** academix_tz.md §2.2 "Sinflar" — admin CRUD over school_classes. */
@Service
public class SchoolClassService {

  private final SchoolClassRepository classRepository;

  public SchoolClassService(SchoolClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  public List<SchoolClass> list(UUID schoolId) {
    return classRepository.findBySchoolIdOrderByGradeAscLetterAsc(schoolId).stream()
        .map(SchoolClassEntity::toDomain)
        .toList();
  }

  public SchoolClass create(UUID schoolId, int grade, String letter, UUID classTeacherId) {
    if (classRepository.existsBySchoolIdAndGradeAndLetter(schoolId, grade, letter)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_CLASS_ALREADY_EXISTS",
          "Bu sinf allaqachon mavjud.",
          "Boshqa harf yoki sinf raqamini tanlang.");
    }

    SchoolClass schoolClass =
        new SchoolClass(
            UUID.randomUUID(),
            schoolId,
            grade,
            letter,
            grade + "-" + letter,
            classTeacherId,
            0,
            currentAcademicYear(),
            true);
    return classRepository.save(SchoolClassEntity.fromDomain(schoolClass)).toDomain();
  }

  public SchoolClass update(
      UUID schoolId, UUID classId, int grade, String letter, UUID classTeacherId) {
    SchoolClassEntity entity = requireOwned(schoolId, classId);
    SchoolClass updated =
        new SchoolClass(
            entity.getId(),
            schoolId,
            grade,
            letter,
            grade + "-" + letter,
            classTeacherId,
            entity.getStudentCount(),
            entity.getAcademicYear(),
            entity.isActive());
    return classRepository.save(SchoolClassEntity.fromDomain(updated)).toDomain();
  }

  public void delete(UUID schoolId, UUID classId) {
    SchoolClassEntity entity = requireOwned(schoolId, classId);
    classRepository.delete(entity);
  }

  private SchoolClassEntity requireOwned(UUID schoolId, UUID classId) {
    return classRepository
        .findByIdAndSchoolId(classId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_CLASS_NOT_FOUND",
                    "Sinf topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
  }

  // No AcademicYear enum/formula is specified anywhere in the spec docs (known gap) — Uzbek
  // school year runs September-August, so a July "today" is still last year's academic year.
  private static String currentAcademicYear() {
    LocalDate today = LocalDate.now();
    int startYear =
        today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
    return startYear + "-" + (startYear + 1);
  }
}
