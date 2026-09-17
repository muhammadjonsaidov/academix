package uz.academixai.wellbeing.infrastructure.legacy;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.wellbeing.application.port.out.TeacherClassRoster;

/**
 * Adapter for {@link TeacherClassRoster} over the legacy class and student-profile repositories.
 */
@Component
public class LegacyTeacherClassRoster implements TeacherClassRoster {

  private final SchoolClassRepository classes;
  private final StudentProfileRepository students;

  public LegacyTeacherClassRoster(
      SchoolClassRepository classes, StudentProfileRepository students) {
    this.classes = classes;
    this.students = students;
  }

  @Override
  public List<UUID> studentIdsOf(UUID schoolId, UUID teacherId) {
    List<SchoolClassEntity> myClasses =
        classes.findBySchoolIdAndClassTeacherId(schoolId, teacherId);
    return myClasses.stream()
        .flatMap(
            schoolClass ->
                students.findByClassIdAndSchoolId(schoolClass.getId(), schoolId).stream())
        .map(StudentProfileEntity::getUserId)
        .toList();
  }
}
