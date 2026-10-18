import {defineConfig} from '@playwright/test';
export default defineConfig({
  testDir: '.', testMatch: 'browser.spec.mjs', timeout: 60000,
  use: {baseURL: 'http://127.0.0.1:8767', viewport: {width: 1100, height: 1250}, headless: true},
  webServer: {command: 'node serve.mjs', url: 'http://127.0.0.1:8767', reuseExistingServer: !process.env.CI},
  reporter: [['list'], ['html', {open: 'never'}]],
});
