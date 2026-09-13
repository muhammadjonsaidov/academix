package uz.academixai.progress.application.port.out;

import java.util.UUID;

/** Authorization boundary for a parent's access to a linked child. */
public interface ParentChildAccess {

  void requireLinkedChild(UUID parentUserId, UUID studentId);
}
