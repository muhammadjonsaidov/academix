package uz.academixai.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Contexts talk to each other through published APIs, never through each other's adapters.
 *
 * <p>The per-context rules in {@link DomainArchitectureTest} already stop a use case from reaching
 * into the legacy tree. This covers the other direction the modular monolith actually regressed on:
 * one migrated context depending on another's {@code infrastructure} package — Wellbeing read
 * Progress' {@code XpHistoryRepository} directly, which works until Progress changes its schema and
 * nothing tells Wellbeing. Same-owner references are fine ({@code learning.infrastructure} may use
 * {@code learning.domain}); it is the cross-context hop that is not.
 */
@AnalyzeClasses(packages = "uz.academixai")
class ContextBoundaryTest {

  /** Contexts that own a vertical slice; the legacy tree is not one of them. */
  private static final List<String> CONTEXTS =
      List.of(
          "identity",
          "school",
          "learning",
          "intelligence",
          "progress",
          "wellbeing",
          "notification",
          "reporting",
          "family");

  private static final String PACKAGE_ROOT = "uz.academixai.";

  @ArchTest
  static final ArchRule noContextDependsOnAnotherContextsInfrastructure =
      ArchRuleDefinition.noClasses()
          .that()
          .resideInAnyPackage(
              CONTEXTS.stream().map(c -> PACKAGE_ROOT + c + "..").toArray(String[]::new))
          .should(reachOnlyItsOwnInfrastructure())
          .because("a context's adapters are private to it; publish a port instead")
          .as("contexts may not depend on another context's infrastructure package");

  private static ArchCondition<JavaClass> reachOnlyItsOwnInfrastructure() {
    return new ArchCondition<>("only reach their own infrastructure package") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        Optional<String> owner = ownerContext(item.getPackageName());
        if (owner.isEmpty()) {
          return;
        }
        for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
          String target = dependency.getTargetClass().getPackageName();
          Optional<String> targetOwner = ownerContext(target);
          if (targetOwner.isPresent()
              && !targetOwner.get().equals(owner.get())
              && target.contains(".infrastructure")) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getName()
                        + " depends on "
                        + dependency.getTargetClass().getName()
                        + " ("
                        + targetOwner.get()
                        + "'s infrastructure) — publish a port on that context instead"));
          }
        }
      }
    };
  }

  private static Optional<String> ownerContext(String packageName) {
    if (!packageName.startsWith(PACKAGE_ROOT)) {
      return Optional.empty();
    }
    String remainder = packageName.substring(PACKAGE_ROOT.length());
    int separator = remainder.indexOf('.');
    String head = separator < 0 ? remainder : remainder.substring(0, separator);
    return CONTEXTS.contains(head) ? Optional.of(head) : Optional.empty();
  }
}
