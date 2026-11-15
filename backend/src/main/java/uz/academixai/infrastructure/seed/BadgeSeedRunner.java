package uz.academixai.infrastructure.seed;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.academixai.progress.domain.Badge;
import uz.academixai.progress.domain.BadgeCriteriaType;
import uz.academixai.progress.infrastructure.persistence.BadgeEntity;
import uz.academixai.progress.infrastructure.persistence.BadgeRepository;

/**
 * academix_tz.md §4 references badges but defines no catalog anywhere — new, judgment call (see
 * ROADMAP.md Sprint 4). The badge catalog is product reference data, so it runs in every profile —
 * it used to be local/dev-only, which left production with an empty catalog (only the demo seed
 * inserted badges) and badges could never unlock. Idempotent: only seeds when the table is empty,
 * so restarting the app doesn't duplicate rows or fight manual edits. The ids are fixed and match
 * infra/seed/bootstrap.sql, so the demo seed's student badges still resolve whichever runs first.
 */
@Component
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
              UUID.fromString("9f5cfa48-92ac-455a-861c-05593908fcc1"),
              "Birinchi qadam",
              "Birinchi 10 XP to'plandi",
              "🌱",
              BadgeCriteriaType.TOTAL_XP,
              10),
          new Badge(
              UUID.fromString("44cc3b1f-b28b-4f80-beb9-84f7f33151f4"),
              "Faol o'quvchi",
              "100 XP to'plandi",
              "⭐",
              BadgeCriteriaType.TOTAL_XP,
              100),
          new Badge(
              UUID.fromString("e437cc85-92ac-455a-861c-05593908fcc1"),
              "XP ustasi",
              "500 XP to'plandi",
              "🏆",
              BadgeCriteriaType.TOTAL_XP,
              500),
          new Badge(
              UUID.fromString("f6ca1486-aed3-4e1e-8fb1-79c9e455ae02"),
              "3 kunlik seriya",
              "3 kun ketma-ket vazifa bajarildi",
              "🔥",
              BadgeCriteriaType.STREAK_DAYS,
              3),
          new Badge(
              UUID.fromString("89ea4a2f-560c-43aa-9887-a04ac128f35e"),
              "Haftalik seriya",
              "7 kun ketma-ket vazifa bajarildi",
              "🔥🔥",
              BadgeCriteriaType.STREAK_DAYS,
              7),
          new Badge(
              UUID.fromString("e45d709e-aed3-4e1e-8fb1-79c9e455ae02"),
              "Chidamli",
              "30 kun ketma-ket vazifa bajarildi",
              "💎",
              BadgeCriteriaType.STREAK_DAYS,
              30));
}
