import { defineConfig, devices } from '@playwright/test';

const storageState = process.env.PIIP_E2E_STORAGE_STATE;

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  reporter: [
    ['list'],
    ['json', { outputFile: 'test-results/playwright-results.json' }],
  ],
  use: {
    baseURL: process.env.PIIP_E2E_BASE_URL ?? 'http://127.0.0.1:4400',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    ...(storageState ? { storageState } : {}),
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
