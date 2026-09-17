package uz.academixai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Non-negotiable baseline for the modular-monolith migration.
 *
 * <p>Each rule is expressed once for every context rather than repeated per package. The migration
 * is incremental, and a rule that has to be copied ten times is a rule that quietly stops covering
 * the eleventh context — which is exactly what the old per-context list had already started doing:
 * Notification, Reporting and Progress' own services had no rule at all, and School's had been
 * duplicated verbatim.
 */
@AnalyzeClasses(packages = "uz.academixai")
class DomainArchitectureTest {

  /**
   * The ten vertical slices, at the layer each rule cares about. The legacy
   * application/domain/infrastructure/interfaces tree is not one of them — it is what they are
   * migrating out of, and it is allowed to depend on itself until it is empty.
   */
  private static final String[] CONTEXT_APPLICATIONS = {
    "uz.academixai.identity.application..",
    "uz.academixai.school.application..",
    "uz.academixai.learning.application..",
    "uz.academixai.intelligence.application..",
    "uz.academixai.progress.application..",
    "uz.academixai.wellbeing.application..",
    "uz.academixai.notification.application..",
    "uz.academixai.reporting.application..",
    "uz.academixai.family.application..",
    "uz.academixai.onboarding.application..",
  };

  /** Same ten slices, whole package this time — every layer the context owns. */
  private static final String[] CONTEXTS = {
    "uz.academixai.identity..",
    "uz.academixai.school..",
    "uz.academixai.learning..",
    "uz.academixai.intelligence..",
    "uz.academixai.progress..",
    "uz.academixai.wellbeing..",
    "uz.academixai.notification..",
    "uz.academixai.reporting..",
    "uz.academixai.family..",
    "uz.academixai.onboarding..",
  };

  private static final String DOMAIN_PACKAGES = "..domain..";

  @ArchTest
  static final ArchRule domainMustNotDependOnFrameworks =
      noClasses()
          .that()
          .resideInAnyPackage(DOMAIN_PACKAGES)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "org.hibernate..",
              "jakarta.servlet..",
              "org.springframework.amqp..",
              "software.amazon.awssdk..")
          .because(
              "domain aggregates and policies must stay independent of delivery and infrastructure");

  /**
   * A use case may depend on its own ports, its domain and the shared kernel — never on JPA
   * adapters, queue producers or storage clients, and never on the legacy application tree.
   *
   * <p>Where a context needed data it did not own, the fix was a port in {@code
   * <context>.application.port.out} implemented by an adapter under {@code
   * <context>.infrastructure}; the adapters that still wrap a legacy repository live in an {@code
   * infrastructure/legacy} package so the remaining debt is visible in one directory instead of
   * spread through the use cases.
   */
  @ArchTest
  static final ArchRule contextApplicationDependsOnPortsNotInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage(CONTEXT_APPLICATIONS)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("context use cases must depend on ports, not technical adapters");

  /**
   * The legacy delivery tree is on its way out, and no context may reach back into it.
   *
   * <p>This is the rule that made the shared error contract worth extracting: until {@code
   * ApiException} moved to {@code shared.error}, every one of the ten contexts imported something
   * from {@code uz.academixai.interfaces} — so a context could not be moved, tested or reasoned
   * about without dragging the old web layer along with it.
   *
   * <p>Note what is deliberately still allowed: a context may depend on the legacy {@code
   * uz.academixai.domain} entities. Splitting those into the contexts that own them is the next
   * migration, and forbidding the dependency before the entities have a home would only push the
   * work into a big-bang rename.
   */
  @ArchTest
  static final ArchRule noContextDependsOnTheLegacyDeliveryTree =
      noClasses()
          .that()
          .resideInAnyPackage(CONTEXTS)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.interfaces..")
          .because(
              "contexts must publish their own inbound adapters, not reuse the legacy web layer");
}
