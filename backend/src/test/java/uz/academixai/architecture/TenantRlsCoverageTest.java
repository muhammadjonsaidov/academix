package uz.academixai.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The tenant-isolation invariant: a table that holds tenant data must be protected by a row-level
 * security policy, or be on the explicitly-justified {@link #NOT_YET_PROTECTED} list.
 *
 * <p>This exists because the gap was invisible: 10 tables were protected and 26 were not, and
 * nothing in the build said so. Reviewing migrations by hand does not scale, and the failure mode
 * is silent (a missing policy looks exactly like working code until someone reads another tenant's
 * data). The list below is therefore not an escape hatch — it is the backlog, with a reason per
 * entry, and adding a new unprotected tenant table fails the build until it is either protected or
 * justified here.
 *
 * <p>See {@code docs/architecture/mvp-production-roadmap.md} ("Majburiy cross-cutting qoidalar"):
 * every tenant-scoped table gets {@code school_id NOT NULL}, a composite index and an RLS policy.
 */
class TenantRlsCoverageTest {

  private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

  private static final Pattern CREATE_TABLE =
      Pattern.compile("CREATE TABLE\\s+([a-z_]+)\\s*\\((.*?)\\)\\s*;", Pattern.DOTALL);

  private static final Pattern ENABLE_RLS =
      Pattern.compile("ALTER TABLE\\s+([a-z_]+)\\s+ENABLE ROW LEVEL SECURITY");

  /** {@code school_id} is sometimes added after the table is created (e.g. V6 on users). */
  private static final Pattern ADD_SCHOOL_ID =
      Pattern.compile("ALTER TABLE\\s+([a-z_]+)\\s+ADD COLUMN\\s+school_id");

  private static final Pattern REFERENCES = Pattern.compile("REFERENCES\\s+([a-z_]+)\\s*\\(");

  /**
   * Tables that hold tenant data but are not yet policy-protected, with the reason each one cannot
   * be protected by simply writing a policy. Every reason names concrete remaining work — the
   * second tenancy slice (school_id backfill + moving the last unscoped workers to {@link
   * uz.academixai.shared.tenancy.TenantScope}) is what empties this list.
   */
  private static final Map<String, String> NOT_YET_PROTECTED = notYetProtected();

  @Test
  void everyTenantTableIsProtectedOrJustified() throws IOException {
    Map<String, String> protectionState = tenantTables();
    List<String> unprotected = new ArrayList<>();
    protectionState.forEach(
        (table, state) -> {
          if (!"RLS".equals(state) && !NOT_YET_PROTECTED.containsKey(table)) {
            unprotected.add(table);
          }
        });

    assertThat(unprotected)
        .as(
            "Tenant tables without an RLS policy and without a justification in "
                + "TenantRlsCoverageTest.NOT_YET_PROTECTED — add a policy in a migration, or state "
                + "the concrete remaining work")
        .isEmpty();
  }

  @Test
  void theBacklogListHasNoStaleEntries() throws IOException {
    Map<String, String> protectionState = tenantTables();
    List<String> stale = new ArrayList<>();
    List<String> alreadyProtected = new ArrayList<>();
    NOT_YET_PROTECTED
        .keySet()
        .forEach(
            table -> {
              if (!protectionState.containsKey(table)) {
                stale.add(table);
              } else if ("RLS".equals(protectionState.get(table))) {
                alreadyProtected.add(table);
              }
            });

    assertThat(stale).as("NOT_YET_PROTECTED names a table that no migration creates").isEmpty();
    assertThat(alreadyProtected)
        .as("These tables are protected now — remove them from NOT_YET_PROTECTED")
        .isEmpty();
  }

  @Test
  void everyJustificationStatesTheRemainingWork() {
    assertThat(NOT_YET_PROTECTED.values())
        .allSatisfy(
            reason ->
                assertThat(reason)
                    .as("A backlog entry needs a reason a future reader can act on")
                    .isNotBlank()
                    .hasSizeGreaterThan(40));
  }

  /**
   * @return table name → "RLS" when a migration enables row-level security for it.
   */
  private static Map<String, String> tenantTables() throws IOException {
    String allMigrations = allMigrations();
    Map<String, String> bodies = createTableBodies(allMigrations);
    Set<String> tenantTables = tenantTables(bodies, allMigrations);

    Map<String, String> state = new LinkedHashMap<>();
    for (String table : tenantTables) {
      state.put(table, hasRls(allMigrations, table) ? "RLS" : "UNPROTECTED");
    }
    return state;
  }

  private static boolean hasRls(String migrations, String table) {
    Matcher matcher = ENABLE_RLS.matcher(migrations);
    while (matcher.find()) {
      if (matcher.group(1).equals(table)) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, String> createTableBodies(String allMigrations) {
    Map<String, String> bodies = new LinkedHashMap<>();
    Matcher matcher = CREATE_TABLE.matcher(allMigrations);
    while (matcher.find()) {
      bodies.put(matcher.group(1), matcher.group(2));
    }
    return bodies;
  }

  /**
   * A table holds tenant data when it carries {@code school_id} (declared or added later), or when
   * it is keyed by — or hangs off — something that does. The relation is transitive on purpose: a
   * grade has no school of its own, but its submission does, so the grade is still tenant data.
   * Global catalogs (e.g. {@code badges}) reference nothing tenant-scoped and stay out.
   */
  private static Set<String> tenantTables(Map<String, String> bodies, String allMigrations) {
    Set<String> tenant = new HashSet<>();
    bodies.forEach(
        (table, body) -> {
          if (body.contains("school_id")) {
            tenant.add(table);
          }
        });
    Matcher added = ADD_SCHOOL_ID.matcher(allMigrations);
    while (added.find()) {
      tenant.add(added.group(1));
    }

    boolean grew = true;
    while (grew) {
      grew = false;
      for (Map.Entry<String, String> table : bodies.entrySet()) {
        if (tenant.contains(table.getKey()) || !referencesTenantTable(table.getValue(), tenant)) {
          continue;
        }
        tenant.add(table.getKey());
        grew = true;
      }
    }
    return tenant;
  }

  private static boolean referencesTenantTable(String body, Set<String> tenant) {
    Matcher matcher = REFERENCES.matcher(body);
    while (matcher.find()) {
      if (tenant.contains(matcher.group(1))) {
        return true;
      }
    }
    return false;
  }

  private static String allMigrations() throws IOException {
    StringBuilder all = new StringBuilder();
    try (Stream<Path> files = Files.list(MIGRATIONS)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".sql")).sorted().toList()) {
        all.append(Files.readString(file)).append('\n');
      }
    }
    return all.toString();
  }

  private static Map<String, String> notYetProtected() {
    Map<String, String> pending = new LinkedHashMap<>();
    pending.put(
        "users",
        "Pre-authentication by definition: login resolves a school FROM this table, so a "
            + "school-scoped policy would make signing in impossible. Reachable only through "
            + "TenantScope.runAsSystem for that narrow indexed lookup.");
    pending.put(
        "schools",
        "Same bootstrap as users: SchoolContextResolver reads schools.admin_id before a tenant is "
            + "known. Revisit together with a dedicated pre-auth access path.");
    pending.put(
        "school_classes",
        "Read during the same pre-auth school resolution and by the nightly behavior job's "
            + "directory query, which still runs unscoped.");
    pending.put(
        "subjects",
        "Read during pre-auth school resolution and by unscoped catalog lookups in scheduled work.");
    pending.put(
        "student_profiles",
        "Needs the nightly directory read (JpaActiveStudentDirectory) moved into a tenant scope "
            + "first; it currently queries every school's students in one unscoped call.");
    pending.put(
        "ai_feedbacks",
        "Child row of homework_submissions with no school_id of its own: needs a backfilled column "
            + "and every insert path to set it before a policy can be written.");
    pending.put(
        "grades", "Child row of homework_submissions: same school_id backfill as ai_feedbacks.");
    pending.put(
        "exam_grades",
        "Child row of exam_submissions: same school_id backfill, written by teacher grading.");
    pending.put(
        "xp_history",
        "Student-keyed with no school_id: the XP writer runs from the AI consumer and from teacher "
            + "grading, so the column must be backfilled and set on both paths.");
    pending.put("student_badges", "Student-keyed with no school_id: same backfill as xp_history.");
    pending.put(
        "handwriting_profiles",
        "Student-keyed biometric data — highest-priority table for the backfill slice. The weekly "
            + "audit now runs as the system role, so only the column work remains.");
    pending.put(
        "psychological_signals",
        "Written by the nightly analyzer outside any scope today: BehaviorAnalysisService must run "
            + "each student's read and writes in one tenant scope before a policy can be added.");
    pending.put(
        "psychology_watchlist",
        "Student-keyed, written from the psychologist request path and read by the nightly job; "
            + "needs the same scope fix as psychological_signals.");
    pending.put(
        "parent_student_links",
        "Parent-to-student link with no school_id: the derived tenant is the student's school, so "
            + "the column must be backfilled from student_profiles.");
    pending.put(
        "teacher_syllabuses",
        "Teacher-keyed: the syllabus ingestion worker consumes a queue message that carries no "
            + "schoolId, so it cannot scope itself yet. Add schoolId to the message first.");
    pending.put(
        "lesson_plans",
        "Teacher-keyed, and generated from a syllabus through the same ingestion pipeline.");
    pending.put(
        "subject_grading_criteria",
        "Teacher-keyed with no school_id: needs the column plus both write paths (teacher CRUD and "
            + "default-criteria creation) to set it.");
    pending.put(
        "student_unique_tasks",
        "Child row of homework_assignments: derive school_id from the assignment, as its insert "
            + "path already knows the assignment.");
    pending.put(
        "syllabus_chunks",
        "Newest tenant data (V44) and the only tenant table with no school_id at all — it derives "
            + "scope through teacher_syllabuses, so it must be backfilled alongside it.");
    pending.put(
        "reports",
        "Has school_id, but ParentReportService reads it outside a tenant scope; that caller must "
            + "move to TenantScope before the policy lands.");
    pending.put(
        "ai_usage_log",
        "Has school_id and is written inside consumer/request scopes; needs only the policy once "
            + "the budget reads in scheduled work are confirmed scoped.");
    pending.put(
        "import_column_mappings",
        "Admin-only import mapping with school_id; needs only the policy.");
    pending.put(
        "outbox_events",
        "Cross-tenant publisher and a nullable school_id (events written during onboarding, before "
            + "a school exists). Needs a design that separates system rows from tenant rows.");
    pending.put(
        "notifications",
        "User-keyed and also written by the standalone telegram-bot worker, which connects with its "
            + "own credentials and no tenant scope. That service needs its own scoped role first.");
    pending.put("notification_preferences", "Same telegram-bot access path as notifications.");
    pending.put(
        "telegram_connections",
        "Read and written exclusively by the standalone telegram-bot worker.");
    return pending;
  }
}
