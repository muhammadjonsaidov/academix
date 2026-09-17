import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Minimal runtime image for the Docker build — see CLAUDE.md "Supporting tooling"
  // (docker-compose deploy, both backend and frontend containerized).
  output: "standalone",

  // The frontend may be built with a RELATIVE NEXT_PUBLIC_API_URL ("/api/v1") — that's what
  // the single-origin cloudflare-tunnel setup (docker-compose.tunnel.yml + nginx-tunnel.conf)
  // requires, since the browser must call the same origin it loaded the page from. But that
  // relative URL also means direct access to localhost:3000 (no tunnel) sends API calls to
  // the Next.js server itself, which has no /api/* routes → HTML 404 → every login failed
  // with a misleading "wrong credentials" error even with correct credentials.
  //
  // This rewrite fixes BOTH modes: /api/* now proxies to the backend whenever a request
  // reaches Next.js directly. In tunnel mode the request never gets here (nginx intercepts
  // /api/ first), so nothing changes there. BACKEND_INTERNAL_URL is the compose-network
  // name in Docker ("http://backend:8080", set in docker-compose.yml); it falls back to
  // localhost:8080 for plain `npm run dev` on the host.
  async rewrites() {
    // || not ??: a set-but-empty ARG/ENV (standalone Docker build without compose) must
    // also fall back — ?? only handles the truly-unset case, and an empty upstream would
    // turn this into a self-rewriting /api/* loop.
    const upstream = process.env.BACKEND_INTERNAL_URL || "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${upstream}/api/:path*` }];
  },

  // Pentest F-08 — the frontend shipped with NO security headers (only X-Powered-By: Next.js).
  // These close the gaps: CSP (stored-XSS blast radius), clickjacking, MIME-sniffing, referrer
  // leakage and permissions. `poweredByHeader: false` below removes the version banner.
  //
  // CSP notes:
  //  - 'unsafe-inline' for script/style is required by Next.js hydration + the pre-paint theme
  //    <script> and shadcn's runtime-injected styles. Keeping it is a conscious trade-off; the
  //    real XSS defense is React's output escaping (no dangerouslySetInnerHTML on user data).
  //  - connect-src is derived from NEXT_PUBLIC_API_URL so BOTH modes work: a relative /api/v1
  //    (tunnel/single-origin) is 'self', an absolute http://localhost:8080 (local dev) is added
  //    explicitly. The SSE stream connects to the same API base.
  async headers() {
    const apiBase = process.env.NEXT_PUBLIC_API_URL || "/api/v1";
    const apiOrigin = apiBase.startsWith("http")
      ? new URL(apiBase).origin
      : "http://localhost:8080";
    const connectSrc = ["'self'", apiOrigin].join(" ");
    // Next's development client uses eval for Fast Refresh/devtools. Keep this
    // allowance out of production, where the optimized client does not need it.
    const scriptSrc =
      process.env.NODE_ENV === "development"
        ? "'self' 'unsafe-inline' 'unsafe-eval'"
        : "'self' 'unsafe-inline'";

    const contentSecurityPolicy = [
      "default-src 'self'",
      `script-src ${scriptSrc}`,
      "style-src 'self' 'unsafe-inline'",
      `img-src 'self' data: blob:`,
      `connect-src ${connectSrc}`,
      "font-src 'self' data:",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'",
    ].join("; ");

    return [
      {
        source: "/(.*)",
        headers: [
          { key: "Content-Security-Policy", value: contentSecurityPolicy },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(), payment=()" },
        ],
      },
    ];
  },

  // Pentest F-14 — stop advertising the runtime version to scanners.
  poweredByHeader: false,
};

export default nextConfig;
