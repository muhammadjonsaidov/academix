package uz.academixai.progress.infrastructure.legacy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.progress.application.port.out.ParentChildAccess;

/**
 * Adapter for Progress' own {@code ParentChildAccess} port: Family's published rule answers the
 * question, Progress never reaches into Family's persistence.
 */
@Component
public class LegacyParentChildAccess implements ParentChildAccess {

  private final uz.academixai.family.application.port.in.ParentChildAccess links;

  public LegacyParentChildAccess(uz.academixai.family.application.port.in.ParentChildAccess links) {
    this.links = links;
  }

  @Override
  public void requireLinkedChild(UUID parentUserId, UUID studentId) {
    links.requireLinkedChild(parentUserId, studentId);
  }
}
