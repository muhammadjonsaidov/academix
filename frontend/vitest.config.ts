import { configDefaults, defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
    // e2e/ belongs to Playwright. Vitest's default include (**/*.spec.ts) otherwise picks the
    // Playwright specs up and they fail with "Playwright Test did not expect test() to be called
    // here" — two runners fighting over the same filename convention. Playwright only looks in
    // e2e/ (playwright.config.ts testDir), so excluding it here separates them cleanly.
    exclude: [...configDefaults.exclude, "e2e/**"],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "."),
    },
  },
});
