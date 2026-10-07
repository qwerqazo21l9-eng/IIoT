import { defineConfig } from "vitest/config";

export default defineConfig({
  esbuild: { jsx: "automatic" },
  test: {
    include: ["src/**/*.test.tsx"],
    pool: "threads",
    poolOptions: { threads: { singleThread: true } },
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
  },
});
