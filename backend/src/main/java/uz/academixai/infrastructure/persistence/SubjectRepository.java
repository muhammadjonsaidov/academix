package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

  List<SubjectEntity> findBySchoolIdOrderByName(UUID schoolId);

  Optional<SubjectEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
