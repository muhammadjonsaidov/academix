package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, UUID> {

  List<SchoolClassEntity> findBySchoolIdOrderByGradeAscLetterAsc(UUID schoolId);

  Optional<SchoolClassEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  List<SchoolClassEntity> findBySchoolIdAndClassTeacherId(UUID schoolId, UUID classTeacherId);

  boolean existsBySchoolIdAndGradeAndLetter(UUID schoolId, int grade, String letter);

  // Bulk import rows carry a human-readable class name (e.g. "9-D"), not a UUID — see
  // BulkImportService.
  Optional<SchoolClassEntity> findBySchoolIdAndFullNameIgnoreCase(UUID schoolId, String fullName);
}
