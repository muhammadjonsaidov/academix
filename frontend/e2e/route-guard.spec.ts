import { expect, test } from "@playwright/test";
import { SignJWT } from "jose";
import { E2E_JWT_SECRET } from "../playwright.config";

/**
 * End-to-end coverage of proxy.ts, the role route-guard — the one piece of frontend logic that is
 * security-shaped (it decides what a browser is allowed to navigate to) and the reason Playwright
 * was chosen over Cypress in the first place: five roles, five auth states.
 *
 * These assert on the RAW HTTP RESPONSE (maxRedirects: 0) rather than on where the browser ends
 * up. That is deliberate: no backend runs in this suite, so any dashboard page that renders would
 * have its client-side session bootstrap fail and redirect to /login on its own. Following
 * redirects would therefore report "/login" for both the pass and the fail case, and the test
 * would look like it was checking the guard while actually checking nothing.
 */

const SECRET = new TextEncoder().encode(E2E_JWT_SECRET);
const WRONG_SECRET = new TextEncoder().encode("a-different-secret-than-the-server-uses-here");

async function authCookie(
  role: string,
  { expiresIn = "15m", secret = SECRET }: { expiresIn?: string; secret?: Uint8Array } = {},
) {
  const token = await new SignJWT({ role })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime(expiresIn)
    .sign(secret);
  return `academix_auth=${token}`;
}

/** Fetches a path without following redirects, so the guard's own response is what's asserted. */
async function guardResponse(baseURL: string | undefined, path: string, cookie?: string) {
  const { request } = await import("@playwright/test");
  const context = await request.newContext({ baseURL });
  const response = await context.get(path, {
    maxRedirects: 0,
    headers: cookie ? { Cookie: cookie } : {},
  });
  const result = { status: response.status(), location: response.headers()["location"] ?? "" };
  await context.dispose();
  return result;
}

test("unauthenticated dashboard access redirects to /login", async ({ baseURL }) => {
  const { status, location } = await guardResponse(baseURL, "/dashboard/teacher");
  expect(status).toBe(307);
  expect(location).toContain("/login");
});

test("a valid role cookie is allowed through to its own dashboard", async ({ baseURL }) => {
  const { status } = await guardResponse(baseURL, "/dashboard/teacher", await authCookie("TEACHER"));
  expect(status).toBe(200);
});

test("a valid cookie for the WRONG role redirects to /dashboard, not /login", async ({
  baseURL,
}) => {
  // Wrong-role is a different outcome from unauthenticated on purpose (frontend TDD §6.2): the
  // user is legitimately logged in, they just picked a URL that isn't theirs.
  const { status, location } = await guardResponse(
    baseURL,
    "/dashboard/admin",
    await authCookie("TEACHER"),
  );
  expect(status).toBe(307);
  expect(location).toContain("/dashboard");
  expect(location).not.toContain("/login");
});

test("a forged cookie signed with the wrong key redirects to /login", async ({ baseURL }) => {
  const forged = await authCookie("ADMIN", { secret: WRONG_SECRET });
  const { status, location } = await guardResponse(baseURL, "/dashboard/admin", forged);
  expect(status).toBe(307);
  expect(location).toContain("/login");
});

test("an EXPIRED but validly-signed cookie is passed through, not bounced to /login", async ({
  baseURL,
}) => {
  // Regression guard. The access token inside lives 15 minutes while the refresh token behind it
  // lives 7 days, and the refresh cookie is path-scoped away from /dashboard so this guard can
  // never see it. Redirecting on expiry logged people out after 15 idle minutes with a fully
  // recoverable session. Reading the role off an expired token is safe only because jose checks
  // the signature before the claims — which the forged-cookie test above pins down.
  const expired = await authCookie("TEACHER", { expiresIn: "-30m" });
  const { status } = await guardResponse(baseURL, "/dashboard/teacher", expired);
  expect(status).toBe(200);
});

test("an expired cookie is still role-checked and cannot cross into another role", async ({
  baseURL,
}) => {
  // The pass-through above must not become a hole: expiry relaxes the freshness check, never the
  // role check.
  const expired = await authCookie("TEACHER", { expiresIn: "-30m" });
  const { status, location } = await guardResponse(baseURL, "/dashboard/admin", expired);
  expect(status).toBe(307);
  expect(location).toContain("/dashboard");
});
