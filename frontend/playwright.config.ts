import { defineConfig, devices } from '@playwright/test';

/**
 * Pruebas E2E sobre el entorno levantado con `docker compose --profile app up`.
 * E2E_BASE_URL: frontend (por defecto http://localhost:4200).
 * MAILPIT_URL: API de Mailpit para leer los correos (por defecto http://localhost:8025).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    locale: 'es-CL',
    timezoneId: 'America/Santiago',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // RNF-USA-01: usable desde 360 px de ancho.
    { name: 'movil', use: { ...devices['Desktop Chrome'], viewport: { width: 360, height: 780 } } },
  ],
});
