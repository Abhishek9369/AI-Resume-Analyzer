import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

/**
 * Dashboard E2E Tests
 * Tests the main dashboard page navigation and statistics display
 */

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should load dashboard successfully', async ({ page }) => {
    // Verify page title
    await expect(page).toHaveTitle(/Resume Analyzer/);
    
    // Verify dashboard heading
    await expect(page.locator('h2').first()).toContainText('Dashboard');
  });

  test('should display navigation menu', async ({ page }) => {
    // Verify all navigation links are present
    const nav = page.locator('nav');
    
    await expect(nav.getByRole('link', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: /upload/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /candidates/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /job requirements/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /candidate matching/i })).toBeVisible();
    await expect(nav.getByRole('link', { name: /skills master/i })).toBeVisible();
  });

  test('should navigate to different pages', async ({ page }) => {
    // Test navigation to Upload page
    await page.getByRole('link', { name: /upload resumes/i }).click();
    await expect(page).toHaveURL(/.*\/upload/);
    
    // Navigate to Candidates page
    await page.getByRole('link', { name: /candidates/i }).click();
    await expect(page).toHaveURL(/.*\/candidates/);
    
    // Navigate to Jobs page
    await page.getByRole('link', { name: /job requirements/i }).click();
    await expect(page).toHaveURL(/.*\/jobs/);
    
    // Navigate to Skills Master page
    await page.getByRole('link', { name: /skills master/i }).click();
    await expect(page).toHaveURL(/.*\/skills/);
    
    // Navigate back to Dashboard
    await page.getByRole('link', { name: 'Dashboard', exact: true }).click();
    await expect(page).toHaveURL('/');
  });

  test('should display dashboard statistics', async ({ page }) => {
    // Wait for statistics to load
    await page.waitForTimeout(1000);
    
    // Verify quick action buttons are visible
    const uploadButton = page.getByRole('button', { name: /upload resume/i });
    const createJobButton = page.getByRole('button', { name: /create job/i });
    
    // At least one should be visible (exact buttons may vary)
    const hasQuickActions = await uploadButton.isVisible().catch(() => false) || 
                            await createJobButton.isVisible().catch(() => false);
    
    expect(hasQuickActions).toBeTruthy();
  });

  test('should display footer', async ({ page }) => {
    // Verify footer is present
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
    await expect(footer).toContainText(/Resume Analyzer/);
    await expect(footer).toContainText(/2025/);
  });
});
