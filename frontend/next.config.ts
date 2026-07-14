import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Minimal runtime image for the Docker build — see CLAUDE.md "Supporting tooling"
  // (docker-compose deploy, both backend and frontend containerized).
  output: "standalone",
};

export default nextConfig;
