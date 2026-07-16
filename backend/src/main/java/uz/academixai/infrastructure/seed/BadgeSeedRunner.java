package uz.academixai.infrastructure.seed;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Badge;
import uz.academixai.domain.BadgeCriteriaType;
import uz.academixai.infrastructure.persistence.BadgeEntity;
import uz.academixai.infrastructure.persistence.BadgeRepository;

/**
 * academix_tz.md §4 references badges but defines no catalog anywhere — new, judgment call (see
 * ROADMAP.md Sprint 4). Local/dev seed data, not a Flyway migration (seed data isn't schema,
 * matches CLAUDE.md's "Supporting tooling" decision). Idempotent: only seeds when the table is
 * empty, so restarting the app doesn't duplicate rows or fight manual edits.
 */
@Component
@Profile({"local", "dev"})
public class BadgeSeedRunner implements CommandLineRunner {

  private final BadgeRepository badgeRepository;

  public BadgeSeedRunner(BadgeRepository badgeRepository) {
    this.badgeRepository = badgeRepository;
  }

  @Override
  public void run(String... args) {
    if (badgeRepository.count() > 0) {
      return;
    }
    for (Badge badge : CATALOG) {
      badgeRepository.save(BadgeEntity.fromDomain(badge));
    }
  }

  private static final List<Badge> CATALOG =
      List.of(
          new Badge(
              UUID.randomUUID(),
              "Birinchi qadam",
              "Birinchi 10 XP to'plandi",
              "🌱",
              BadgeCriteriaType.TOTAL_XP,
              10),
          new Badge(
              UUID.randomUUID(),
              "Faol o'quvchi",
              "100 XP to'plandi",
              "⭐",
              BadgeCriteriaType.TOTAL_XP,
              100),
          new Badge(
              UUID.randomUUID(),
              "XP ustasi",
              "500 XP to'plandi",
              "🏆",
              BadgeCriteriaType.TOTAL_XP,
              500),
          new Badge(
              UUID.randomUUID(),
              "3 kunlik seriya",
              "3 kun ketma-ket vazifa bajarildi",
              "🔥",
              BadgeCriteriaType.STREAK_DAYS,
              3),
          new Badge(
              UUID.randomUUID(),
              "Haftalik seriya",
              "7 kun ketma-ket vazifa bajarildi",
              "🔥🔥",
              BadgeCriteriaType.STREAK_DAYS,
              7),
          new Badge(
              UUID.randomUUID(),
              "Chidamli",
              "30 kun ketma-ket vazifa bajarildi",
              "💎",
              BadgeCriteriaType.STREAK_DAYS,
              30));
}
