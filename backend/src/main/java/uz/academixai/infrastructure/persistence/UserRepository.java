package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.domain.Role;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByPhone(String phone);

  Optional<UserEntity> findFirstByEmailIgnoreCase(String email);

  boolean existsByPhone(String phone);

  List<UserEntity> findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role role, UUID schoolId);

  Optional<UserEntity> findByIdAndRoleAndSchoolId(UUID id, Role role, UUID schoolId);

  int countByRoleAndSchoolId(Role role, UUID schoolId);
}
