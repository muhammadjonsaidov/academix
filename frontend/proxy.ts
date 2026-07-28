import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { decodeJwt, jwtVerify } from "jose";

// Next.js 16 renamed middleware.ts -> proxy.ts (function middleware() -> proxy()); edge
// runtime dropped, proxy always runs on nodejs. See CLAUDE.md "Reality checks" — this
// supersedes academix_frontend_tdd.md §6.2's literal "middleware.ts" wording, but the
// verification logic below is unchanged from what that section specifies.
//
// UX-only route-flash prevention, not real authorization — the backend's @PreAuthorize
// is what actually gates data. This reads a SIGNED httpOnly cookie (can't be forged
// without the secret) rather than a plain readable role cookie (devtools-editable).
//
// Signing key: raw JWT_SECRET bytes, matching JwtService.java exactly — both sides must
// derive the same key from the same secret, or signature verification always fails.
//
// Resolved lazily, per request, rather than at module scope: this file is bundled at build
// time, when JWT_SECRET (a runtime-only var) may legitimately not be set yet — a top-level
// throw would fail `npm run build` in Docker. Failing here instead means a misconfigured
// deploy surfaces as a loud, named error in the server log, not as the silent symptom it
// used to produce: TextEncoder().encode(undefined) yields an empty key, every jwtVerify
// fails, and every dashboard request redirects to /login with a valid session and no clue
// why. See CLAUDE.md — a mismatched JWT_SECRET has this same signature.
function signingKey(): Uint8Array {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error(
      "JWT_SECRET is not set for the frontend. proxy.ts cannot verify the academix_auth " +
        "cookie without it, and every /dashboard request would redirect to /login. Set it in " +
        "frontend/.env.local, byte-for-byte identical to the backend's.",
    );
  }
  return new TextEncoder().encode(secret);
}

const ROLE_PATH_PREFIXES: Record<string, string> = {
  "/dashboard/admin": "ADMIN",
  "/dashboard/teacher": "TEACHER",
  "/dashboard/student": "STUDENT",
  "/dashboard/parent": "PARENT",
  "/dashboard/psychologist": "PSYCHOLOGIST",
};

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const authCookie = request.cookies.get("academix_auth")?.value;

  if (pathname.startsWith("/dashboard") && !authCookie) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  let userRole: string | undefined;
  if (authCookie) {
    // Deliberately resolved OUTSIDE the try: a missing JWT_SECRET is a deployment fault, not a
    // bad cookie, and must not be swallowed into the same "redirect to /login" path that would
    // hide it (which is exactly what made the original symptom so hard to diagnose).
    const key = signingKey();
    try {
      const { payload } = await jwtVerify(authCookie, key);
      userRole = payload.role as string;
    } catch (error) {
      // Expired is NOT treated like a bad signature any more. The access token inside this
      // cookie lasts 15 minutes, but the refresh token behind it lasts 7 days — and because
      // academix_refresh is path-scoped to /api/v1/auth, it is never sent here, so this guard
      // cannot see that a recoverable session exists. Redirecting on expiry therefore logged
      // people out after 15 idle minutes with a perfectly valid session, before the client-side
      // bootstrap that exists to restore it could run.
      //
      // Reading the role off an expired token is safe specifically because jose verifies the
      // SIGNATURE before it validates claims: getting ERR_JWT_EXPIRED (rather than a signature
      // error) is itself proof the cookie was issued by us and hasn't been tampered with. A
      // forged or edited cookie fails earlier and still redirects below.
      if ((error as { code?: string }).code !== "ERR_JWT_EXPIRED") {
        return NextResponse.redirect(new URL("/login", request.url));
      }
      userRole = decodeJwt(authCookie).role as string;
    }
  }

  for (const [prefix, requiredRole] of Object.entries(ROLE_PATH_PREFIXES)) {
    if (pathname.startsWith(prefix) && userRole !== requiredRole) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
