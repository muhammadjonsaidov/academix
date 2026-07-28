import { defineConfig, devices } from "@playwright/test";

/** Shared by the webServer below and e2e/route-guard.spec.ts — see the `env` note. */
export const E2E_JWT_SECRET = "e2e-only-jwt-secret-not-used-by-any-real-environment";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: "html",
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
  webServer: {
    // next.config.ts sets output:"standalone", for which `next start` is not the supported
    // entrypoint (it prints "does not work with output: standalone" and is not something CI
    // should depend on). The standalone server is a self-contained bundle that does NOT include
    // static assets, so .next/static and public/ have to be copied in beside it.
    command:
      "npm run build && cp -r .next/static .next/standalone/.next/ && cp -r public .next/standalone/ && node .next/standalone/server.js",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    // proxy.ts verifies the academix_auth cookie against this exact secret, and the guard specs
    // sign their own cookies with it. Must be identical on both sides or every request redirects
    // to /login and the tests fail for the wrong reason. A throwaway test value, never a real key.
    env: { JWT_SECRET: E2E_JWT_SECRET },
  },
});
