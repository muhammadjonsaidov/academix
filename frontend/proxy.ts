import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtVerify } from "jose";

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
const JWT_SECRET = new TextEncoder().encode(process.env.JWT_SECRET);

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
    try {
      const { payload } = await jwtVerify(authCookie, JWT_SECRET);
      userRole = payload.role as string;
    } catch {
      // Invalid signature or expired — same handling either way.
      return NextResponse.redirect(new URL("/login", request.url));
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
