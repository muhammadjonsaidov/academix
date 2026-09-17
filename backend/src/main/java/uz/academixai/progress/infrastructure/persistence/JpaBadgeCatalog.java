package uz.academixai.progress.infrastructure.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import uz.academixai.progress.application.port.out.BadgeCatalog;
import uz.academixai.progress.domain.Badge;

/** JPA adapter for badge definitions. */
@Repository
public class JpaBadgeCatalog implements BadgeCatalog {

  private final BadgeRepository badges;

  public JpaBadgeCatalog(BadgeRepository badges) {
    this.badges = badges;
  }

  @Override
  public List<Badge> findAll() {
    return badges.findAll().stream().map(BadgeEntity::toDomain).toList();
  }
}
