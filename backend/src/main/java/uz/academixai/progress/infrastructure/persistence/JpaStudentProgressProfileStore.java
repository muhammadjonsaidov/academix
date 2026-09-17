package uz.academixai.progress.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.progress.application.port.out.StudentProgressProfileStore;

/** Transitional School profile adapter for Progress snapshots. */
@Repository
public class JpaStudentProgressProfileStore implements StudentProgressProfileStore {

  private final StudentProfileRepository profiles;

  public JpaStudentProgressProfileStore(StudentProfileRepository profiles) {
    this.profiles = profiles;
  }

  @Override
  public Optional<StudentProfile> findByStudentId(UUID studentId) {
    return profiles.findByUserId(studentId).map(StudentProfileEntity::toDomain);
  }

  @Override
  public StudentProfile save(StudentProfile profile) {
    return profiles.save(StudentProfileEntity.fromDomain(profile)).toDomain();
  }
}
