package uz.academixai.family.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.family.domain.ParentStudentLink;

/**
 * Family's published API to other contexts.
 *
 * <p>Progress and School consume this instead of Family's repositories or entity classes: the
 * question "is this really that parent's child" is Family's rule to answer, and the JWT's single
 * {@code schoolId} claim can never answer it (a parent may have children at two schools).
 */
public interface ParentChildAccess {

  void requireLinkedChild(UUID parentUserId, UUID studentId);

  List<ParentStudentLink> childrenOf(UUID parentUserId);

  /** Active parents of one student — Wellbeing needs this to route a CRITICAL alert. */
  List<ParentStudentLink> parentsOf(UUID studentId);
}
