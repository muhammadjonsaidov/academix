package uz.academixai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Non-negotiable baseline for the modular-monolith migration.
 *
 * <p>Context migration is incremental, so legacy application services can still have temporary
 * infrastructure dependencies. Domain code cannot: putting Spring, JPA or transport concerns in an
 * aggregate makes every later port/adapter migration more expensive and couples business rules to a
 * framework. New context domain packages and the legacy domain package are both protected.
 */
@AnalyzeClasses(packages = "uz.academixai")
class DomainArchitectureTest {

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

  @ArchTest
  static final ArchRule identityApplicationMustDependOnPortsNotInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("uz.academixai.identity.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("Identity application use cases must depend on ports, not technical adapters");

  @ArchTest
  static final ArchRule schoolApplicationMustDependOnPortsNotInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("uz.academixai.school.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("School application use cases must depend on ports, not technical adapters");

  @ArchTest
  static final ArchRule learningApplicationMustDependOnPortsNotInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("uz.academixai.learning.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("Learning application use cases must depend on ports, not technical adapters");

  @ArchTest
  static final ArchRule xpPolicyMustDependOnPortsNotInfrastructure =
      noClasses()
          .that()
          .haveFullyQualifiedName("uz.academixai.progress.application.XPService")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("Progress XP policy must use its persistence ports, not JPA adapters directly");

  @ArchTest
  static final ArchRule intelligenceApplicationMustDependOnPortsNotInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("uz.academixai.intelligence.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("uz.academixai.infrastructure..", "uz.academixai.application..")
          .because("Intelligence policies must depend on provider and configuration ports");
}
