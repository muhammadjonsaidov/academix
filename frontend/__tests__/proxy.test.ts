// @vitest-environment node
//
// Node, not the project-default jsdom: proxy.ts runs on Next.js 16's nodejs runtime (the edge
// runtime was dropped for proxy), so this matches production. It is also required — under jsdom,
// TextEncoder returns a Uint8Array from a different JS realm and jose rejects it outright with
// "payload must be an instance of Uint8Array".
import { beforeAll, describe, expect, test } from "vitest";
import { SignJWT, decodeJwt, jwtVerify } from "jose";

/**
 * The route-guard lets an EXPIRED cookie through (so a 15-minute-idle user isn't logged out while
 * a valid 7-day refresh token exists) and reads the role off it without re-verifying. That is only
 * safe if jose validates the signature BEFORE it validates claims — i.e. if ERR_JWT_EXPIRED can
 * only ever come from a token we really issued. These tests pin that assumption down, because if a
 * future jose release ever checked `exp` first, the guard would start trusting forged cookies and
 * nothing else in the codebase would notice.
 */
const SECRET = new TextEncoder().encode("test-secret-matching-backend-jwt-secret-value");
const WRONG_SECRET = new TextEncoder().encode("a-completely-different-signing-key-value-here");

async function issue(secret: Uint8Array, expiresAt: string) {
  return new SignJWT({ role: "TEACHER" })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt(Math.floor(Date.now() / 1000) - 3600)
    .setExpirationTime(expiresAt)
    .sign(secret);
}

describe("proxy.ts expired-cookie handling", () => {
  let expiredValid: string;
  let expiredForged: string;

  beforeAll(async () => {
    expiredValid = await issue(SECRET, "-30m"); // signed by us, expired 30 min ago
    expiredForged = await issue(WRONG_SECRET, "-30m"); // expired AND signed by someone else
  });

  test("an expired but correctly-signed token fails with ERR_JWT_EXPIRED", async () => {
    await expect(jwtVerify(expiredValid, SECRET)).rejects.toMatchObject({
      code: "ERR_JWT_EXPIRED",
    });
  });

  test("a forged token fails on the SIGNATURE, never with ERR_JWT_EXPIRED", async () => {
    // This is the load-bearing assertion: if this ever returns ERR_JWT_EXPIRED, the guard's
    // pass-through branch would accept an attacker-authored cookie.
    await expect(jwtVerify(expiredForged, SECRET)).rejects.not.toMatchObject({
      code: "ERR_JWT_EXPIRED",
    });
    await expect(jwtVerify(expiredForged, SECRET)).rejects.toMatchObject({
      code: "ERR_JWS_SIGNATURE_VERIFICATION_FAILED",
    });
  });

  test("a valid unexpired token verifies and carries the role claim", async () => {
    const fresh = await issue(SECRET, "15m");
    const { payload } = await jwtVerify(fresh, SECRET);
    expect(payload.role).toBe("TEACHER");
  });

  test("decodeJwt recovers the role from the expired token the guard passes through", () => {
    expect(decodeJwt(expiredValid).role).toBe("TEACHER");
  });
});
