import { describe, expect, it } from "vitest";
import { dashboardPageTitle, isNavItemActive } from "@/components/shared/nav-config";

describe("dashboard navigation model", () => {
  it("uses the most specific navigation section for nested routes", () => {
    expect(
      dashboardPageTitle("TEACHER", "/dashboard/teacher/homework/abc-123/review"),
    ).toBe("Uy vazifalari");
  });

  it("does not mark a role overview active for every nested route", () => {
    expect(isNavItemActive("/dashboard/admin/classes", "/dashboard/admin")).toBe(false);
  });
});
