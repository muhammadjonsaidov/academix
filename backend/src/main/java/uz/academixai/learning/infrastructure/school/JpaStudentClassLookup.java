package uz.academixai.learning.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.learning.application.port.out.StudentClassLookup;

/** Transitional School membership adapter for student submission authorization. */
@Repository
public class JpaStudentClassLookup implements StudentClassLookup {

  private final StudentProfileRepository profiles;

  public JpaStudentClassLookup(StudentProfileRepository profiles) {
    this.profiles = profiles;
  }

  @Override
  public Optional<UUID> classId(UUID schoolId, UUID studentId) {
    return profiles
        .findByUserIdAndSchoolId(studentId, schoolId)
        .map(StudentProfileEntity::toDomain)
        .map(profile -> profile.classId());
  }
}
