package uz.academixai.family.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.family.application.port.out.ParentLinkStore;
import uz.academixai.family.domain.ParentRelation;
import uz.academixai.family.domain.ParentStudentLink;

/** JPA adapter for Family's own {@code parent_student_links} table. */
@Repository
public class JpaParentLinkStore implements ParentLinkStore {

  private final ParentStudentLinkRepository links;

  public JpaParentLinkStore(ParentStudentLinkRepository links) {
    this.links = links;
  }

  @Override
  public Optional<ParentStudentLink> find(UUID parentUserId, UUID studentId) {
    return links.findByParentUserIdAndStudentUserId(parentUserId, studentId).map(this::toDomain);
  }

  @Override
  public ParentStudentLink save(ParentStudentLink link) {
    return links.save(ParentStudentLinkEntity.fromDomain(link)).toDomain();
  }

  @Override
  public boolean existsActive(UUID parentUserId, UUID studentId) {
    return links.existsByParentUserIdAndStudentUserIdAndIsActiveTrue(parentUserId, studentId);
  }

  @Override
  public List<ParentStudentLink> findActiveByParent(UUID parentUserId) {
    return links.findByParentUserIdAndIsActiveTrue(parentUserId).stream()
        .map(ParentStudentLinkEntity::toDomain)
        .toList();
  }

  @Override
  public List<ParentStudentLink> findActiveByStudent(UUID studentId) {
    return links.findByStudentUserIdAndIsActiveTrue(studentId).stream()
        .map(ParentStudentLinkEntity::toDomain)
        .toList();
  }

  @Override
  public List<LinkedChild> findActiveChildrenOfSchool(UUID schoolId) {
    return links.findActiveChildrenBySchool(schoolId).stream().map(this::toLinkedChild).toList();
  }

  @Override
  public List<LinkedChild> findActiveChildrenOfParent(UUID parentUserId) {
    return links.findActiveChildrenByParent(parentUserId).stream()
        .map(this::toLinkedChild)
        .toList();
  }

  private ParentStudentLink toDomain(ParentStudentLinkEntity entity) {
    return entity.toDomain();
  }

  private LinkedChild toLinkedChild(ParentStudentLinkRepository.ChildRow row) {
    return new LinkedChild(
        row.getParentUserId(),
        row.getStudentUserId(),
        ParentRelation.valueOf(row.getRelation()),
        row.getFirstName(),
        row.getLastName(),
        row.getClassName());
  }
}
