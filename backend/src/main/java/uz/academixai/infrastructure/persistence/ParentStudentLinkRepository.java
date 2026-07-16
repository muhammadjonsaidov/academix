package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLinkEntity, UUID> {

  List<ParentStudentLinkEntity> findByParentUserIdAndIsActiveTrue(UUID parentUserId);

  List<ParentStudentLinkEntity> findByStudentUserIdAndIsActiveTrue(UUID studentUserId);

  Optional<ParentStudentLinkEntity> findByParentUserIdAndStudentUserId(
      UUID parentUserId, UUID studentUserId);

  boolean existsByParentUserIdAndStudentUserIdAndIsActiveTrue(
      UUID parentUserId, UUID studentUserId);
}
