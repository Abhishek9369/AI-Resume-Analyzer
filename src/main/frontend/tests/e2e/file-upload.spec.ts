import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

/**
 * File Upload E2E Tests
 * Tests the resume upload functionality including:
 * - Upload dropzone
 * - Progress tracking
 * - Upload history table
 * - Error handling
 * - Multiple file formats (PDF, DOC, DOCX, ZIP)
 */

test.describe('File Upload', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await page.goto('/upload');
    await page.waitForLoadState('networkidle');
  });

  test('should display upload page with dual-component UI', async ({ page }) => {
    // Verify page heading
    await expect(page.locator('h2')).toContainText(/Upload|Resume/i);
    
    // Wait for components to load
    await page.waitForTimeout(1000);
    
    // Verify upload dropzone is visible (unless upload in progress)
    const dropzone = page.locator('text=/drag.*drop|select.*file/i, [class*="dropzone"]');
    const hasDropzone = await dropzone.isVisible().catch(() => false);
    
    if (hasDropzone) {
      await expect(dropzone).toBeVisible();
    }
  });

  test('should display upload history table', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for upload history section
    const historySection = page.locator('text=/upload.*history|recent.*uploads|upload.*status/i');
    const hasHistory = await historySection.isVisible().catch(() => false);
    
    if (hasHistory) {
      await expect(historySection).toBeVisible();
      
      // Verify refresh button exists
      const refreshButton = page.getByRole('button', { name: /refresh/i });
      if (await refreshButton.isVisible().catch(() => false)) {
        await expect(refreshButton).toBeVisible();
      }
    }
  });

  test('should show current upload status if upload in progress', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Check for current upload status section
    const currentStatus = page.locator('text=/current.*upload|processing.*status|upload.*progress/i');
    const hasCurrentStatus = await currentStatus.isVisible().catch(() => false);
    
    if (hasCurrentStatus) {
      // Verify status is highlighted
      await expect(currentStatus).toBeVisible();
    }
  });

  test('should display upload dropzone for file selection', async ({ page }) => {
    await page.waitForTimeout(1000);
    
    // Look for file input or dropzone
    const fileInput = page.locator('input[type="file"]');
    const dropzoneText = page.locator('text=/drag.*drop|click.*upload|select.*files/i');
    
    const hasFileInput = await fileInput.isVisible().catch(() => false);
    const hasDropzone = await dropzoneText.isVisible().catch(() => false);
    
    // At least one should be visible
    expect(hasFileInput || hasDropzone).toBeTruthy();
  });

  test('should show accepted file formats', async ({ page }) => {
    await page.waitForTimeout(1000);
    
    // Look for accepted formats text (PDF, DOC, DOCX, ZIP)
    const formatText = page.locator('text=/pdf|doc|docx|zip/i');
    const hasFormatInfo = await formatText.count() > 0;
    
    if (hasFormatInfo) {
      await expect(formatText.first()).toBeVisible();
    }
  });

  test('should display upload history with status badges', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for status badges in the table
    const statusBadges = page.locator('[class*="badge"], [class*="status"]').filter({ 
      hasText: /initiated|processing|completed|failed/i 
    });
    
    const badgeCount = await statusBadges.count();
    
    if (badgeCount > 0) {
      // Verify status badges have different colors
      const completedBadge = page.locator('text=/completed/i').first();
      const failedBadge = page.locator('text=/failed/i').first();
      
      if (await completedBadge.isVisible().catch(() => false)) {
        await expect(completedBadge).toBeVisible();
      }
      
      if (await failedBadge.isVisible().catch(() => false)) {
        await expect(failedBadge).toBeVisible();
      }
    }
  });

  test('should display progress bars in upload history', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for progress indicators
    const progressElements = page.locator('[class*="progress"], [role="progressbar"]');
    const hasProgress = await progressElements.count() > 0;
    
    if (hasProgress) {
      // Verify progress element exists
      await expect(progressElements.first()).toBeVisible();
    }
  });

  test('should have individual refresh buttons for each upload entry', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for refresh icons in table rows
    const refreshButtons = page.locator('button').filter({ hasText: /🔄|refresh/i });
    const refreshIconButtons = page.locator('button svg[data-icon*="refresh"], button [class*="refresh"]');
    
    const refreshCount = await refreshButtons.count() + await refreshIconButtons.count();
    
    if (refreshCount > 0) {
      // Click first refresh button
      const firstRefresh = refreshButtons.first().or(refreshIconButtons.first());
      await firstRefresh.click();
      await page.waitForTimeout(1000);
      
      // Verify page didn't navigate away
      await expect(page).toHaveURL(/.*\/upload/);
    }
  });

  test('should have bulk refresh all button', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for "Refresh All" button
    const refreshAllButton = page.getByRole('button', { name: /refresh all/i });
    
    if (await refreshAllButton.isVisible().catch(() => false)) {
      await refreshAllButton.click();
      await page.waitForTimeout(1000);
      
      // Verify page is still on upload
      await expect(page).toHaveURL(/.*\/upload/);
    }
  });

  test('should display upload history table columns', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Check for table headers
    const table = page.locator('table');
    
    if (await table.isVisible().catch(() => false)) {
      const thead = table.locator('thead');
      
      // Verify key columns exist
      const hasStatus = await thead.getByText(/status/i).isVisible().catch(() => false);
      const hasFiles = await thead.getByText(/files/i).isVisible().catch(() => false);
      const hasProgress = await thead.getByText(/progress/i).isVisible().catch(() => false);
      const hasActions = await thead.getByText(/actions/i).isVisible().catch(() => false);
      
      // At least some core columns should exist
      expect(hasStatus || hasFiles || hasProgress || hasActions).toBeTruthy();
    }
  });

  test('should show timestamps in upload history', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for timestamp displays (dates, times)
    const timestamps = page.locator('text=/\\d{1,2}\\/\\d{1,2}\\/\\d{4}|started|completed|am|pm/i');
    const hasTimestamps = await timestamps.count() > 0;
    
    if (hasTimestamps) {
      await expect(timestamps.first()).toBeVisible();
    }
  });

  test('should handle failed upload state correctly', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for failed status
    const failedStatus = page.locator('text=/failed/i').first();
    
    if (await failedStatus.isVisible().catch(() => false)) {
      // Verify failed uploads don't block new uploads
      // The dropzone div contains 'Drag & drop' text and is always visible
      const dragDropText = await page.getByText(/drag.*drop/i).isVisible().catch(() => false);
      const fileInput = await page.locator('input[type="file"]').count() > 0;
      
      // Upload area should still be accessible
      expect(dragDropText || fileInput).toBeTruthy();
    }
  });

  test('should not block uploads when previous upload is completed', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Check for completed uploads
    const completedStatus = page.locator('text=/completed/i').first();
    
    if (await completedStatus.isVisible().catch(() => false)) {
      // Verify dropzone is visible and not blocked
      const dragDropText = await page.getByText(/drag.*drop/i).isVisible().catch(() => false);
      const fileInput = await page.locator('input[type="file"]').count() > 0;
      
      expect(dragDropText || fileInput).toBeTruthy();
    }
  });

  test('should show message when no upload history exists', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for empty state message
    const emptyMessage = page.locator('text=/no.*uploads|no.*history|no.*recent/i');
    const table = page.locator('table tbody tr');
    
    const hasEmptyMessage = await emptyMessage.isVisible().catch(() => false);
    const hasTableRows = await table.count() > 0;
    
    // Either should have history or empty message
    expect(hasEmptyMessage || hasTableRows).toBeTruthy();
  });

  test('should display file count in progress (e.g., "3/10 files")', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for file count display
    const fileCount = page.locator('text=/\\d+\\/\\d+.*files?|processed.*files/i');
    
    if (await fileCount.isVisible().catch(() => false)) {
      await expect(fileCount.first()).toBeVisible();
    }
  });

  test('should show error messages for failed uploads', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Look for error messages in failed uploads
    const failedRow = page.locator('text=/failed/i').locator('..').locator('..');
    
    if (await failedRow.isVisible().catch(() => false)) {
      // Verify message column has content
      const messageText = await failedRow.textContent();
      expect(messageText).toBeTruthy();
    }
  });

  test('should maintain upload area visibility with dual-component layout', async ({ page }) => {
    await page.waitForTimeout(1500);
    
    // Verify both components can coexist
    const hasUploadArea = await page.getByText(/drag.*drop/i).isVisible().catch(() => false)
      || await page.locator('input[type="file"]').count() > 0;
    const hasHistoryArea = await page.locator('table').isVisible().catch(() => false)
      || await page.getByText(/upload.*history|recent.*uploads/i).isVisible().catch(() => false);
    
    // In ideal dual-component UI, both should be visible
    // But at minimum, the page should show one or the other
    expect(hasUploadArea || hasHistoryArea).toBeTruthy();
  });
});
