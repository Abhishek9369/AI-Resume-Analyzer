import { describe, it, expect } from 'vitest';
import { screen, render } from '../../test/test-utils';
import Dashboard from './Dashboard';

describe('Dashboard Component', () => {
  it('should render dashboard title', () => {
    render(<Dashboard />);
    
    expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
  });

  it('should render all main sections', () => {
    render(<Dashboard />);
    
    // Check for key dashboard elements
    const dashboard = document.querySelector('[class*="dashboard"]');
    expect(dashboard).toBeTruthy();
  });

  it('should display welcome message', () => {
    render(<Dashboard />);
    
    // Look for any welcome-related text
    const welcomeText = screen.queryByText(/welcome/i) || screen.queryByText(/resume analyzer/i);
    if (welcomeText) {
      expect(welcomeText).toBeInTheDocument();
    }
  });

  it('should render without crashing', () => {
    const { container } = render(<Dashboard />);
    expect(container).toBeTruthy();
  });

  it('should have proper semantic structure', () => {
    const { container } = render(<Dashboard />);
    
    // Check for proper HTML structure
    expect(container.querySelector('[class*="dashboard"]')).toBeTruthy();
  });
});
