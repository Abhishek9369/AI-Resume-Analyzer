import { Page } from '@playwright/test';

/**
 * Shared authentication helper for E2E tests.
 * Logs in as admin unless a specific user is requested.
 */
export async function loginAs(
  page: Page,
  username = 'admin',
  password = 'Admin@123'
): Promise<void> {
  // Navigate to app root — will redirect to /login if not authenticated
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  // If we're already logged in, skip
  const currentUrl = page.url();
  if (!currentUrl.includes('/login') && !currentUrl.endsWith('/')) {
    return;
  }

  // Go to login page explicitly
  await page.goto('/login');
  await page.waitForSelector('#username, input[placeholder*="username" i]', { timeout: 15000 });

  // Fill credentials
  const usernameInput = page.locator('#username, input[placeholder*="username" i]').first();
  const passwordInput = page.locator('#password, input[placeholder*="password" i]').first();

  await usernameInput.fill(username);
  await passwordInput.fill(password);

  await page.getByRole('button', { name: /sign in|login/i }).click();

  // Wait for redirect away from login
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });
  await page.waitForLoadState('networkidle');
}
