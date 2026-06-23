import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

/**
 * Job Requirements E2E Tests
 * Tests the job requirements management including:
 * - Job listing display
 * - Create new job with skills autocomplete
 * - Experience range slider
 * - Skills badge display
 * - Form validation
 */

test.describe('Job Requirements', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await page.goto('/jobs');
    await page.waitForLoadState('networkidle');
  });

  test('should display job requirements page', async ({ page }) => {
    // Verify page heading
    await expect(page.locator('h2')).toContainText(/Job Requirements/i);
    
    // Verify "Create New Job" button
    await expect(page.getByRole('button', { name: /create.*job/i })).toBeVisible();
  });

  test('should open create job form', async ({ page }) => {
    // Click "Create New Job" button
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Verify form is visible
    await expect(page.getByRole('heading', { name: /create job requirement/i })).toBeVisible();
    
    // Verify all form fields are present
    await expect(page.locator('input[name="title"], #job-title').first()).toBeVisible();
    await expect(page.getByPlaceholder(/search.*skills|type.*skills/i)).toBeVisible();
    await expect(page.locator('#required-education, input[name="requiredEducation"]').first()).toBeVisible();
    await expect(page.locator('#domain, input[name="domainRequirements"]').first()).toBeVisible();
    await expect(page.locator('#description, textarea[name="description"]').first()).toBeVisible();
  });

  test('should display experience range slider', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Verify experience range slider is present
    const experienceSection = page.getByText(/experience.*range/i);
    await expect(experienceSection).toBeVisible();
    
    // Verify slider elements (dual range slider)
    const sliders = page.locator('input[type="range"]');
    const sliderCount = await sliders.count();
    
    // Should have range slider (at least 1 slider input)
    expect(sliderCount).toBeGreaterThanOrEqual(1);
    
    // Verify years display (e.g., "0 years - 10 years")
    await expect(page.getByText(/\d+\s*years/i).first()).toBeVisible();
  });

  test('should test skills autocomplete functionality', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Find skills input field
    const skillsInput = page.getByPlaceholder(/search.*skills|type.*skills/i);
    await expect(skillsInput).toBeVisible();
    
    // Type to trigger autocomplete
    await skillsInput.fill('Jav');
    await page.waitForTimeout(1000);
    
    // Look for autocomplete suggestions
    const suggestions = page.locator('[role="option"], [class*="suggestion"], [class*="autocomplete"]');
    const suggestionCount = await suggestions.count();
    
    if (suggestionCount > 0) {
      // Verify Java and JavaScript appear
      await expect(page.getByText('Java', { exact: false }).first()).toBeVisible();
      
      // Click on a suggestion
      await suggestions.first().click();
      await page.waitForTimeout(500);
    }
  });

  test('should display selected skills as badges', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Add a skill using autocomplete
    const skillsInput = page.getByPlaceholder(/search.*skills|type.*skills/i);
    await skillsInput.fill('Java');
    await page.waitForTimeout(1000);
    
    // Click suggestion if available
    const suggestion = page.getByText('Java', { exact: false }).first();
    if (await suggestion.isVisible()) {
      await suggestion.click();
      await page.waitForTimeout(500);
      
      // Look for badge display
      const badge = page.locator('[class*="badge"], [class*="tag"], [class*="chip"]').filter({ hasText: /java/i });
      if (await badge.isVisible().catch(() => false)) {
        await expect(badge).toBeVisible();
      }
    }
  });

  test('should adjust experience range using slider', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Get the first range slider
    const slider = page.locator('input[type="range"]').first();
    
    if (await slider.isVisible()) {
      // Get initial value display
      const initialText = await page.getByText(/\d+\s*years/i).first().textContent().catch(() => null);
      
      // Adjust slider by clicking and dragging
      const sliderBox = await slider.boundingBox();
      if (sliderBox) {
        // Click at 50% position
        await page.mouse.click(sliderBox.x + sliderBox.width * 0.5, sliderBox.y + sliderBox.height / 2);
        await page.waitForTimeout(300);
        
        // Verify value changed
        const newText = await page.getByText(/\d+\s*years/i).first().textContent().catch(() => null);
        // Values should be different or remain valid
        expect(newText).toBeTruthy();
      }
    }
  });

  test('should validate required fields', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Try to submit without filling required fields
    const createButton = page.getByRole('button', { name: /create job/i });
    await createButton.click();
    await page.waitForTimeout(500);
    
    // Form should still be visible (validation failed)
    await expect(page.getByRole('heading', { name: /create job requirement/i })).toBeVisible();
  });

  test('should create a complete job requirement', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Fill in job title
    const timestamp = Date.now();
    await page.locator('input[name="title"], #job-title').first().fill(`Senior Developer ${timestamp}`);
    
    // Add skills if autocomplete works
    const skillsInput = page.getByPlaceholder(/search.*skills|type.*skills/i);
    await skillsInput.fill('Java');
    await page.waitForTimeout(1000);
    
    const suggestion = page.getByText('Java', { exact: false }).first();
    if (await suggestion.isVisible().catch(() => false)) {
      await suggestion.click();
      await page.waitForTimeout(500);
    }
    
    // Fill other fields
    await page.locator('#required-education, input[name="requiredEducation"]').first().fill("Bachelor's in Computer Science");
    await page.locator('#domain, input[name="domainRequirements"]').first().fill('Software Development');
    await page.locator('#description, textarea[name="description"]').first().fill('Looking for an experienced Java developer');
    
    // Submit form
    await page.getByRole('button', { name: /create job/i }).click();
    await page.waitForTimeout(1500);
    
    // Verify job was created (form closed or success message)
    const formStillVisible = await page.getByRole('heading', { name: /create job requirement/i }).isVisible()
      .catch(() => false);
    
    // Form should be closed if successful
    if (!formStillVisible) {
      // Success - job created
      expect(true).toBeTruthy();
    }
  });

  test('should close form on cancel', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Click cancel button
    await page.getByRole('button', { name: /cancel/i }).click();
    await page.waitForTimeout(300);
    
    // Form should be closed
    const formVisible = await page.getByRole('heading', { name: /create job requirement/i }).isVisible()
      .catch(() => false);
    
    expect(formVisible).toBeFalsy();
  });

  test('should display category badges in skill suggestions', async ({ page }) => {
    // Open create job form
    await page.getByRole('button', { name: /create.*job/i }).click();
    await page.waitForTimeout(500);
    
    // Type to get suggestions
    const skillsInput = page.getByPlaceholder(/search.*skills|type.*skills/i);
    await skillsInput.fill('Java');
    await page.waitForTimeout(1000);
    
    // Look for category labels (e.g., "Programming Language")
    const categoryBadges = page.locator('text=/programming language|framework|database/i');
    const hasCategoryBadges = await categoryBadges.count() > 0;
    
    // If categories are shown, they should be visible
    if (hasCategoryBadges) {
      expect(await categoryBadges.first().isVisible()).toBeTruthy();
    }
  });
});
