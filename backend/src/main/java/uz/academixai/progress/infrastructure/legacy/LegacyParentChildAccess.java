package uz.academixai.progress.infrastructure.legacy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.ParentLinkService;
import uz.academixai.progress.application.port.out.ParentChildAccess;

/**
 * Transitional authorization adapter until parent-child links are migrated to the Family context.
 */
@Component
public class LegacyParentChildAccess implements ParentChildAccess {

  private final ParentLinkService links;

  public LegacyParentChildAccess(ParentLinkService links) {
    this.links = links;
  }

  @Override
  public void requireLinkedChild(UUID parentUserId, UUID studentId) {
    links.requireLinkedChild(parentUserId, studentId);
  }
}
