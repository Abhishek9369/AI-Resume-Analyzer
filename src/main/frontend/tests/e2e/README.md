# E2E Test Suite

This directory contains comprehensive end-to-end (E2E) tests for the Resume Analyzer application using Playwright.

## Test Coverage

### Test Files

| Test File | Test Cases | Coverage |
|-----------|------------|----------|
| `dashboard.spec.ts` | 7 | Dashboard navigation, statistics display, page routing |
| `skills-master.spec.ts` | 10 | Skills CRUD operations, pagination, filtering, icon buttons |
| `job-requirements.spec.ts` | 11 | Job creation, skills autocomplete, experience slider, badges |
| `file-upload.spec.ts` | 18 | Resume upload, progress tracking, dual-component UI, history table |
| `candidates.spec.ts` | 23 | Candidate listing, search, filtering, pagination, actions |
| `candidate-matching.spec.ts` | 20 | Job-to-candidate matching, match scores, skill highlighting |
| **Total** | **89** | **Complete application workflow** |

### Features Tested

#### 1. Dashboard (`dashboard.spec.ts`)
- Dashboard page loading
- Navigation menu verification (all 6 pages)
- Page navigation functionality
- Statistics display
- Footer presence

#### 2. Skills Master (`skills-master.spec.ts`)
- Skills table display with pagination
- Create new skill modal
- Edit existing skills
- Delete skills with confirmation
- Filter by active/inactive status
- Icon buttons verification (FontAwesome/Lucide icons)
- Search functionality
- Category dropdown
- Form validation

#### 3. Job Requirements (`job-requirements.spec.ts`)
- Job requirements page display
- Create job form modal
- Experience range slider (min/max values)
- Skills autocomplete with dynamic search
- Category badges in skill suggestions
- Selected skills display as badges with remove buttons
- Required fields validation
- Complete job creation workflow
- Form cancellation
- Slider adjustment interactions

#### 4. File Upload (`file-upload.spec.ts`)
- Dual-component UI (upload area + history table)
- Upload dropzone display
- Accepted file formats (PDF, DOC, DOCX, ZIP)
- Upload progress tracking
- File count display (e.g., "3/10 files")
- Upload history table with columns
- Status badges (Initiated, Processing, Completed, Failed)
- Progress bars for in-progress uploads
- Individual and bulk refresh buttons
- Timestamps display
- Error message handling
- Empty state messages

#### 5. Candidates List (`candidates.spec.ts`)
- Candidates table with all columns (name, email, experience, skills)
- Search/filter functionality
- Skills display as badges
- Experience years display
- Pagination controls (next, previous, page size)
- Action buttons (view, edit, delete)
- Delete confirmation dialog
- Email addresses display
- Resume filename/document reference
- Total count display
- Empty state handling
- Loading state

#### 6. Candidate Matching (`candidate-matching.spec.ts`)
- Job selection dropdown
- Match candidates button (enabled/disabled states)
- Match results table display
- Match scores as percentages
- Sorting by match score (descending)
- Matched skills highlighting
- Missing/unmatched skills display
- Required skills vs. candidate skills comparison
- Job requirements summary
- Re-run matching with different jobs
- Loading state during matching
- No matches found handling
- Export/action buttons

## Running Tests

### Prerequisites

1. **Install dependencies:**
   ```bash
   cd src/main/frontend
   yarn install
   ```

2. **Install Playwright browsers:**
   ```bash
   npx playwright install
   ```

3. **Start the application:**
   The tests expect the application to be running at `https://localhost`. Use Docker:
   ```bash
   cd docker
   docker-compose up
   ```

### Test Commands

#### Run all E2E tests (headless)
```bash
yarn test:e2e
```

#### Run tests with UI mode (interactive)
```bash
yarn test:e2e:ui
```

#### Run tests in headed mode (see browser)
```bash
yarn test:e2e:headed
```

#### Run tests in debug mode
```bash
yarn test:e2e:debug
```

#### Run specific test file
```bash
npx playwright test dashboard.spec.ts
```

#### Run specific test case
```bash
npx playwright test -g "should display dashboard page"
```

### Browser Configuration

Tests run on multiple browsers by default:
- **Chromium** (Desktop)
- **Firefox** (Desktop)
- **WebKit** (Desktop - Safari engine)
- **Mobile Chrome** (Android simulation)
- **Mobile Safari** (iOS simulation)

To run on a specific browser:
```bash
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
npx playwright test --project="Mobile Chrome"
```

## Test Configuration

The test suite is configured in `playwright.config.ts`:

- **Base URL:** `https://localhost`
- **Parallel execution:** Yes
- **Retries:** 2 (on CI), 0 (locally)
- **Timeout:** 30 seconds per test
- **Trace:** On first retry
- **Screenshots:** On failure
- **Video:** On failure
- **Reporters:** HTML, JSON, List

### HTTPS Self-Signed Certificates

The application uses self-signed certificates for HTTPS. The test configuration includes:
```typescript
ignoreHTTPSErrors: true
```

This allows tests to run against the local HTTPS server without certificate validation errors.

## Test Patterns

### Page Object Pattern

While these tests use direct locators, consider implementing Page Object Model for better maintainability:

```typescript
// Example: pages/DashboardPage.ts
export class DashboardPage {
  constructor(private page: Page) {}
  
  async goto() {
    await this.page.goto('/');
  }
  
  async navigateToSkillsMaster() {
    await this.page.getByRole('link', { name: /skills master/i }).click();
  }
}
```

### Conditional Testing

Many tests use conditional logic to handle:
- Empty states
- Loading states
- Dynamic content
- Optional features

Example:
```typescript
const hasTable = await table.isVisible().catch(() => false);
if (hasTable) {
  // Test table functionality
}
```

### Wait Strategies

Tests use multiple wait strategies:
- `waitForLoadState('networkidle')` - Wait for network to be idle
- `waitForTimeout(ms)` - Fixed wait (use sparingly)
- `waitForSelector()` - Wait for element to appear
- Auto-waiting built into Playwright actions

## Viewing Test Results

### HTML Report

After test execution, view the HTML report:
```bash
npx playwright show-report
```

### Traces

If tests fail, view the trace for debugging:
```bash
npx playwright show-trace trace.zip
```

Traces include:
- Screenshot of each action
- Network requests
- Console logs
- DOM snapshots
- Timeline

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd src/main/frontend
          yarn install
      
      - name: Install Playwright browsers
        run: npx playwright install --with-deps
      
      - name: Start application
        run: |
          cd docker
          docker-compose up -d
      
      - name: Wait for application
        run: npx wait-on https://localhost
      
      - name: Run E2E tests
        run: |
          cd src/main/frontend
          yarn test:e2e
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: src/main/frontend/playwright-report/
```

## Debugging Tests

### VS Code Debugging

1. Install the **Playwright Test for VSCode** extension
2. Open a test file
3. Click the green play button next to a test
4. Or use the Testing sidebar

### Inspector

Run tests with the Playwright Inspector:
```bash
npx playwright test --debug
```

This opens:
- Browser window
- Inspector with step-by-step execution
- Console for running commands

### Headed Mode

See the browser while tests run:
```bash
yarn test:e2e:headed
```

### Slow Motion

Run tests in slow motion:
```bash
npx playwright test --headed --slow-mo=1000
```

## Best Practices

1. **Avoid hard-coded waits** - Use Playwright's auto-waiting instead of `waitForTimeout`
2. **Use role-based selectors** - `getByRole()` is more robust than CSS selectors
3. **Test user flows, not implementation** - Focus on what users see and do
4. **Keep tests independent** - Each test should work in isolation
5. **Use page fixtures** - Playwright provides clean page state for each test
6. **Handle flakiness** - Use retries and proper wait strategies
7. **Verify happy and unhappy paths** - Test both success and error scenarios

## Known Issues & Limitations

1. **Self-signed certificates** - Tests require `ignoreHTTPSErrors: true`
2. **Upload functionality** - File upload tests may require actual test files in the future
3. **Dynamic content** - Some tests use conditional logic to handle varying data states
4. **GraphQL timing** - Some GraphQL queries may need longer timeouts

## Future Improvements

- [ ] Add Page Object Model pattern for better maintainability
- [ ] Create test data fixtures for consistent test state
- [ ] Add visual regression testing with snapshot comparison
- [ ] Implement API mocking for isolated frontend tests
- [ ] Add accessibility testing (WCAG compliance)
- [ ] Add performance testing (Lighthouse integration)
- [ ] Create helper utilities for common test operations
- [ ] Add test coverage reporting integration

## Resources

- [Playwright Documentation](https://playwright.dev)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright API Reference](https://playwright.dev/docs/api/class-playwright)
- [Debugging Guide](https://playwright.dev/docs/debug)

## Troubleshooting

### Tests fail with "Target closed" error
- Ensure the application is running at `https://localhost`
- Check Docker containers are healthy: `docker ps`

### Tests timeout
- Increase timeout in `playwright.config.ts`
- Check network connectivity
- Verify GraphQL backend is responding

### Browser installation issues
```bash
# On Linux, install system dependencies
npx playwright install-deps

# Then install browsers
npx playwright install
```

### Port conflicts
- Ensure port 443 is available for HTTPS
- Check no other services are using the port
- Verify Docker port mappings

## Contributing

When adding new tests:

1. Follow existing test structure
2. Use descriptive test names
3. Add comments for complex test logic
4. Update this README with new test coverage
5. Ensure tests pass on all browsers
6. Handle both success and error scenarios

---

**Test Suite Version:** 1.0  
**Last Updated:** February 16, 2026  
**Playwright Version:** ^1.41.0
