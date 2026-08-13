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
};

export default nextConfig;
