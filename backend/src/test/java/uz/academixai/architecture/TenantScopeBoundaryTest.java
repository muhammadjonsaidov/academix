package uz.academixai.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps tenancy from drifting back into ad-hoc call sites.
 *
 * <p>{@code SET LOCAL app.current_school_id} / {@code SET LOCAL ROLE} used to be duplicated across
 * six classes (the request filter, two queue listeners, the exam batch acceptor, the Wellbeing
 * activity lookup, and — the case that actually broke — a scheduled audit that had no scope at
 * all). Each copy was a place a future caller could forget. They now all go through {@link
 * uz.academixai.shared.tenancy.TenantScope}, and this test is what keeps it that way: a raw
 * statement anywhere outside the adapter fails the build.
 *
 * <p>ArchUnit cannot see string literals, so this scans the source instead. It reads main sources
 * only, so tests are free to set roles directly (that is exactly what the isolation tests do).
 */
class TenantScopeBoundaryTest {

  private static final Path SOURCE_ROOT = Path.of("src/main/java");

  /** The one package allowed to talk to Postgres about tenancy. */
  private static final Set<String> ADAPTER_PACKAGE = Set.of("uz/academixai/infrastructure/tenancy");

  private static final List<String> FORBIDDEN_STATEMENTS = List.of("SET LOCAL", "SET ROLE");

  @Test
  void onlyTheTenancyAdapterMayChangeTenantSettings() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> sources = Files.walk(SOURCE_ROOT)) {
      for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
        String relative = SOURCE_ROOT.relativize(source).toString().replace('\\', '/');
        if (ADAPTER_PACKAGE.stream().anyMatch(relative::startsWith)) {
          continue;
        }
        String code = withoutComments(Files.readString(source));
        if (FORBIDDEN_STATEMENTS.stream().anyMatch(code::contains)) {
          offenders.add(relative);
        }
      }
    }

    assertThat(offenders)
        .as(
            "Tenant settings must be applied through TenantScope "
                + "(uz.academixai.shared.tenancy), never with a raw SET LOCAL/SET ROLE")
        .isEmpty();
  }

  @Test
  void theTenancyAdapterActuallyExists() {
    assertThat(Files.isDirectory(SOURCE_ROOT.resolve(ADAPTER_PACKAGE.iterator().next()))).isTrue();
  }

  /** Removes block and line comments so prose about the settings is not mistaken for code. */
  private static String withoutComments(String source) {
    StringBuilder stripped = new StringBuilder(source.length());
    boolean inBlockComment = false;
    for (String line : source.split("\n", -1)) {
      String trimmed = line.trim();
      if (inBlockComment) {
        int end = trimmed.indexOf("*/");
        if (end < 0) {
          continue;
        }
        inBlockComment = false;
        stripped.append(trimmed.substring(end + 2)).append('\n');
        continue;
      }
      if (trimmed.startsWith("/*")) {
        int end = trimmed.indexOf("*/");
        if (end < 0) {
          inBlockComment = true;
          continue;
        }
        stripped.append(trimmed.substring(end + 2)).append('\n');
        continue;
      }
      if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
        continue;
      }
      int lineComment = line.indexOf("//");
      stripped.append(lineComment < 0 ? line : line.substring(0, lineComment)).append('\n');
    }
    return stripped.toString();
  }
}
