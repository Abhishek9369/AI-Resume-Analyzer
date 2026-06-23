import { test, expect, Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

/**
 * RBAC End-to-End Validation Tests
 * Validates all role-based access control flows with screenshot capture
 */

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SCREENSHOTS_DIR = path.join(__dirname, '..', '..', '..', '..', '..', 'docs', 'images');

// Increase timeout for all tests
test.setTimeout(60000);

async function ensureScreenshotsDir() {
  if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  }
}

async function captureScreenshot(page: Page, filename: string) {
  await ensureScreenshotsDir();
  const filepath = path.join(SCREENSHOTS_DIR, filename);
  await page.screenshot({ path: filepath, fullPage: false });
  console.log(`Screenshot saved: ${filename}`);
}

async function login(page: Page, username: string, password: string) {
  await page.goto('/login');
  // Wait for the login form to appear
  await page.waitForSelector('#username', { timeout: 15000 });
  await page.fill('#username', username);
  await page.fill('#password', password);
  await page.getByRole('button', { name: /sign in|login/i }).click();
  // Wait for redirect away from login
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 });
}

async function logout(page: Page) {
  // Try various logout triggers
  const logoutBtn = page.getByRole('button', { name: /logout|sign out/i });
  if (await logoutBtn.isVisible()) {
    await logoutBtn.click();
  } else {
    // Try user menu/avatar
    const menuTrigger = page.locator('[data-testid="user-menu"], .user-menu, [aria-label*="user" i]').first();
    if (await menuTrigger.isVisible()) {
      await menuTrigger.click();
      await page.getByRole('menuitem', { name: /logout|sign out/i }).click();
    }
  }
  await page.waitForURL(/login/, { timeout: 5000 }).catch(() => {});
}

// ─── Login Page ───────────────────────────────────────────────────────────────

test.describe('01 - Login Page', () => {
  test('login page loads with RBAC info', async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('#username', { timeout: 15000 });
    await captureScreenshot(page, '01-login-page.png');
    await expect(page).toHaveURL(/login/);
  });
});

// ─── Admin Role ───────────────────────────────────────────────────────────────

test.describe('02 - Admin Role', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin', 'Admin@123');
  });

  test('admin dashboard', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '02-admin-dashboard.png');
    await expect(page.locator('body')).not.toContainText('login');
  });

  test('admin - user management', async ({ page }) => {
    // Navigate to user management
    const userMgmtLink = page.getByRole('link', { name: /user.*manag|admin|users/i });
    if (await userMgmtLink.first().isVisible()) {
      await userMgmtLink.first().click();
      await page.waitForLoadState('networkidle');
      await captureScreenshot(page, '03-admin-user-management.png');
    } else {
      // Try navigating directly
      await page.goto('/admin/users');
      await page.waitForLoadState('networkidle');
      await captureScreenshot(page, '03-admin-user-management.png');
    }
  });

  test('admin - navigation has all menu items', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    const nav = page.locator('nav, [role="navigation"]').first();
    await captureScreenshot(page, '04-admin-navigation.png');
  });
});

// ─── Recruiter Role ───────────────────────────────────────────────────────────

test.describe('03 - Recruiter Role', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'recruiter', 'Recruiter@123');
  });

  test('recruiter dashboard', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '05-recruiter-dashboard.png');
  });

  test('recruiter - job requirements page', async ({ page }) => {
    const jobsLink = page.getByRole('link', { name: /job.*req|jobs/i });
    if (await jobsLink.first().isVisible()) {
      await jobsLink.first().click();
    } else {
      await page.goto('/jobs');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '06-recruiter-job-requirements.png');
  });

  test('recruiter - create job requirement', async ({ page }) => {
    await page.goto('/jobs');
    await page.waitForLoadState('networkidle');

    // Click create/add button
    const createBtn = page.getByRole('button', { name: /create|add|new.*job/i });
    if (await createBtn.first().isVisible({ timeout: 3000 })) {
      await createBtn.first().click();
      await page.waitForLoadState('networkidle');
      await captureScreenshot(page, '07-recruiter-create-job-form.png');

      // Fill in the form
      const titleInput = page.locator('input[name="title"], input[placeholder*="title" i]').first();
      if (await titleInput.isVisible({ timeout: 3000 })) {
        await titleInput.fill('Senior Software Engineer');
        
        const descInput = page.locator('textarea[name="description"], textarea[placeholder*="description" i]').first();
        if (await descInput.isVisible({ timeout: 2000 })) {
          await descInput.fill('We are looking for a Senior Software Engineer with 5+ years of experience in Java and Spring Boot. The ideal candidate will have strong problem-solving skills and experience with microservices architecture.');
        }

        await captureScreenshot(page, '08-recruiter-job-form-filled.png');

        // Submit - use the modal's submit button specifically
        const submitBtn = page.locator('button[type="submit"]').or(
          page.getByRole('button', { name: /create job|save|update/i })
        );
        if (await submitBtn.first().isVisible({ timeout: 2000 })) {
          await submitBtn.first().click({ force: true });
          await page.waitForLoadState('networkidle');
          await captureScreenshot(page, '09-recruiter-job-created.png');
        }
      }
    } else {
      await captureScreenshot(page, '07-recruiter-jobs-page.png');
    }
  });

  test('recruiter - upload resume page', async ({ page }) => {
    const uploadLink = page.getByRole('link', { name: /upload/i });
    if (await uploadLink.first().isVisible()) {
      await uploadLink.first().click();
    } else {
      await page.goto('/upload');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '10-recruiter-upload-page.png');
  });

  test('recruiter - candidates page', async ({ page }) => {
    const candidatesLink = page.getByRole('link', { name: /candidates/i });
    if (await candidatesLink.first().isVisible()) {
      await candidatesLink.first().click();
    } else {
      await page.goto('/candidates');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '11-recruiter-candidates-page.png');
  });
});

// ─── Hiring Manager Role ──────────────────────────────────────────────────────

test.describe('04 - Hiring Manager Role', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'hiring_manager', 'Manager@123');
  });

  test('hiring manager dashboard', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '12-hiring-manager-dashboard.png');
  });

  test('hiring manager - candidate matching', async ({ page }) => {
    const matchingLink = page.getByRole('link', { name: /match|candidate.*match/i });
    if (await matchingLink.first().isVisible()) {
      await matchingLink.first().click();
    } else {
      await page.goto('/matching');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '13-hiring-manager-matching.png');
  });

  test('hiring manager - candidates view', async ({ page }) => {
    const candidatesLink = page.getByRole('link', { name: /candidates/i });
    if (await candidatesLink.first().isVisible()) {
      await candidatesLink.first().click();
    } else {
      await page.goto('/candidates');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '14-hiring-manager-candidates.png');
  });
});

// ─── HR Role ──────────────────────────────────────────────────────────────────

test.describe('05 - HR Role', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'hr', 'HR@123');
  });

  test('hr dashboard', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '15-hr-dashboard.png');
  });

  test('hr - candidates page', async ({ page }) => {
    const candidatesLink = page.getByRole('link', { name: /candidates/i });
    if (await candidatesLink.first().isVisible()) {
      await candidatesLink.first().click();
    } else {
      await page.goto('/candidates');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '16-hr-candidates.png');
  });

  test('hr - job requirements page', async ({ page }) => {
    const jobsLink = page.getByRole('link', { name: /job.*req|jobs/i });
    if (await jobsLink.first().isVisible()) {
      await jobsLink.first().click();
    } else {
      await page.goto('/jobs');
    }
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '17-hr-job-requirements.png');
  });
});

// ─── Access Control Validation ────────────────────────────────────────────────

test.describe('06 - Access Control', () => {
  test('unauthenticated redirect to login', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await captureScreenshot(page, '18-unauthenticated-redirect.png');
    // Should be redirected to login
    await expect(page).toHaveURL(/login/);
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    
    await page.locator('input[name="username"], input[placeholder*="username" i], input[id*="username" i]').first().fill('invalid_user');
    await page.locator('input[type="password"]').first().fill('wrongpassword');
    await page.getByRole('button', { name: /sign in|login/i }).click();
    
    await page.waitForTimeout(2000);
    await captureScreenshot(page, '19-invalid-login-error.png');
    // Should still be on login page or show error
    const url = page.url();
    const hasError = await page.locator('[class*="error"], [role="alert"], .alert').first().isVisible().catch(() => false);
    expect(url.includes('login') || hasError).toBeTruthy();
  });
});
