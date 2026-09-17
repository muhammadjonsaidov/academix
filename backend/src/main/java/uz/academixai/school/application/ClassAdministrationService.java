package uz.academixai.school.application;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.school.application.port.out.ClassAdministrationRepository;

/** Class aggregate administration use cases for one school tenant. */
@Service
public class ClassAdministrationService {

  private final ClassAdministrationRepository classes;

  public ClassAdministrationService(ClassAdministrationRepository classes) {
    this.classes = classes;
  }

  public List<SchoolClass> list(UUID schoolId) {
    return classes.findBySchoolId(schoolId);
  }

  public SchoolClass create(UUID schoolId, int grade, String letter, UUID classTeacherId) {
    if (classes.existsBySchoolIdAndGradeAndLetter(schoolId, grade, letter)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_CLASS_ALREADY_EXISTS",
          "Bu sinf allaqachon mavjud.",
          "Boshqa harf yoki sinf raqamini tanlang.");
    }

    return classes.save(
        new SchoolClass(
            UUID.randomUUID(),
            schoolId,
            grade,
            letter,
            grade + "-" + letter,
            classTeacherId,
            0,
            currentAcademicYear(),
            true));
  }

  public SchoolClass update(
      UUID schoolId, UUID classId, int grade, String letter, UUID classTeacherId) {
    SchoolClass existing = requireOwned(schoolId, classId);
    return classes.save(
        new SchoolClass(
            existing.id(),
            schoolId,
            grade,
            letter,
            grade + "-" + letter,
            classTeacherId,
            existing.studentCount(),
            existing.academicYear(),
            existing.isActive()));
  }

  public void delete(UUID schoolId, UUID classId) {
    requireOwned(schoolId, classId);
    classes.deleteById(classId);
  }

  private SchoolClass requireOwned(UUID schoolId, UUID classId) {
    return classes
        .findByIdAndSchoolId(classId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_CLASS_NOT_FOUND",
                    "Sinf topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
  }

  private static String currentAcademicYear() {
    LocalDate today = LocalDate.now();
    int startYear =
        today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
    return startYear + "-" + (startYear + 1);
  }
}
