package uz.academixai.school.infrastructure.legacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository.StudentListRow;
import uz.academixai.school.application.port.out.StudentStore;

/**
 * Compatibility adapter for {@code student_profiles}, which lives in the legacy persistence package
 * because Progress' XP writes and Wellbeing's activity reads still reach it directly. Ownership
 * moves here as those two switch to published APIs.
 */
@Component
public class LegacyStudentStore implements StudentStore {

  private final StudentProfileRepository profiles;

  public LegacyStudentStore(StudentProfileRepository profiles) {
    this.profiles = profiles;
  }

  @Override
  public List<StudentRow> search(UUID schoolId, UUID classId, String search) {
    return profiles.searchBySchool(schoolId, classId, search).stream()
        .map(LegacyStudentStore::toRow)
        .toList();
  }

  @Override
  public Optional<StudentProfile> findInSchool(UUID studentUserId, UUID schoolId) {
    return profiles
        .findByUserIdAndSchoolId(studentUserId, schoolId)
        .map(StudentProfileEntity::toDomain);
  }

  @Override
  public StudentProfile save(StudentProfile profile) {
    return profiles.save(StudentProfileEntity.fromDomain(profile)).toDomain();
  }

  private static StudentRow toRow(StudentListRow row) {
    return new StudentRow(
        row.getUserId(),
        row.getFirstName(),
        row.getLastName(),
        row.getPhone(),
        row.getClassId(),
        row.getStudentNumber(),
        row.getBirthDate(),
        row.getIsActive());
  }
}
