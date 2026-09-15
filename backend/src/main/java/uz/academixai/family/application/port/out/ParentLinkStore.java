package uz.academixai.family.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.family.domain.ParentRelation;
import uz.academixai.family.domain.ParentStudentLink;

/** Persistence port for parent↔child links — Family owns {@code parent_student_links}. */
public interface ParentLinkStore {

  Optional<ParentStudentLink> find(UUID parentUserId, UUID studentId);

  ParentStudentLink save(ParentStudentLink link);

  boolean existsActive(UUID parentUserId, UUID studentId);

  List<ParentStudentLink> findActiveByParent(UUID parentUserId);

  List<ParentStudentLink> findActiveByStudent(UUID studentId);

  List<LinkedChild> findActiveChildrenOfSchool(UUID schoolId);

  List<LinkedChild> findActiveChildrenOfParent(UUID parentUserId);

  /**
   * A linked child as the admin/parent lists need it: who the child is, which class, and how the
   * parent is related. Replaces the JPA projection that used to leak out of the repository.
   */
  record LinkedChild(
      UUID parentUserId,
      UUID studentUserId,
      ParentRelation relation,
      String firstName,
      String lastName,
      String className) {}
}
