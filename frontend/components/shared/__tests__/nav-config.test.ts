import { describe, expect, it } from "vitest";
import { dashboardPageTitle, isNavItemActive, NAV_CONFIG } from "@/components/shared/nav-config";
import { isPsychologyRoute } from "@/lib/features";

describe("dashboard navigation model", () => {
  it("uses the most specific navigation section for nested routes", () => {
    expect(
      dashboardPageTitle("TEACHER", "/dashboard/teacher/homework/abc-123"),
    ).toBe("Uy vazifalari");
  });

  it("does not mark a role overview active for every nested route", () => {
    expect(isNavItemActive("/dashboard/admin/classes", "/dashboard/admin")).toBe(false);
  });

  it("hides the psychology add-on module's navigation while it is off (the default)", () => {
    // The PSYCHOLOGIST role's own menu is unreachable while the module is off (sign-in refused).
    const hrefs = (["ADMIN", "TEACHER", "STUDENT", "PARENT"] as const)
      .flatMap((role) => NAV_CONFIG[role])
      .flatMap((group) => group.items.map((item) => item.href));
    expect(hrefs.filter(isPsychologyRoute)).toEqual([]);
  });
});
