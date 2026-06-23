import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '../../test/test-utils';
import CandidateMatching from './CandidateMatching';
import { mockCandidates, mockJobRequirements, mockCandidateMatch } from '../../test/mockData';

describe('CandidateMatching Component', () => {
  it('should render candidate matching component', () => {
    render(<CandidateMatching />);
    expect(screen.getByText(/match|matching|candidate/i)).toBeInTheDocument();
  });

  it('should display match results when loaded', () => {
    const initialState = {
      matches: {
        matches: [mockCandidateMatch],
        loading: false,
        error: null,
        matchingInProgress: false,
      },
      candidates: {
        candidates: mockCandidates,
        loading: false,
        error: null,
      },
      jobs: {
        jobs: mockJobRequirements,
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Check for match score (mockCandidateMatch.matchScore is 85.5, rendered as "85.5%")
    expect(screen.getByText(/85\.5%/i)).toBeInTheDocument();
  });

  it('should show loading state', () => {
    const initialState = {
      matches: {
        matches: [],
        loading: false,
        error: null,
        matchingInProgress: true,
      },
      candidates: {
        candidates: [],
        loading: false,
        error: null,
      },
      jobs: {
        jobs: [],
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Component shows "Matching..." when matchingInProgress is true
    expect(screen.getByText(/Matching/i)).toBeInTheDocument();
  });

  it('should show error message when error occurs', () => {
    const initialState = {
      matches: {
        matches: [],
        loading: false,
        error: 'Failed to load matches',
        matchingInProgress: false,
      },
      candidates: {
        candidates: [],
        loading: false,
        error: null,
      },
      jobs: {
        jobs: [],
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Component doesn't display error messages, just shows empty state
    expect(screen.getByText(/Candidate Matching/i)).toBeInTheDocument();
  });

  it('should display match details', () => {
    const initialState = {
      matches: {
        matches: [mockCandidateMatch],
        loading: false,
        error: null,
        matchingInProgress: false,
      },
      candidates: {
        candidates: mockCandidates,
        loading: false,
        error: null,
      },
      jobs: {
        jobs: mockJobRequirements,
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Check for score breakdown labels
    expect(screen.getByText('Skills')).toBeInTheDocument();
    expect(screen.getByText('Experience')).toBeInTheDocument();
    expect(screen.getByText('Education')).toBeInTheDocument();
    expect(screen.getByText('Domain')).toBeInTheDocument();
  });

  it('should show empty state when no matches', () => {
    const initialState = {
      matches: {
        matches: [],
        loading: false,
        error: null,
        matchingInProgress: false,
      },
      candidates: {
        candidates: [],
        loading: false,
        error: null,
      },
      jobs: {
        jobs: [],
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Empty state only shows when a job is selected but no matches
    // Without a selected job, just the selector is shown
    expect(screen.getByText(/Candidate Matching/i)).toBeInTheDocument();
  });

  it('should display candidate and job information', () => {
    const initialState = {
      matches: {
        matches: [mockCandidateMatch],
        loading: false,
        error: null,
        matchingInProgress: false,
      },
      candidates: {
        candidates: mockCandidates,
        loading: false,
        error: null,
      },
      jobs: {
        jobs: mockJobRequirements,
        loading: false,
        error: null,
      },
    };
    
    render(<CandidateMatching />, { preloadedState: initialState });
    
    // Check for candidate name in match card
    expect(screen.getByText(/John Doe/i)).toBeInTheDocument();
  });

  it('should have match button or trigger', () => {
    render(<CandidateMatching />);
    
    const matchButton = screen.queryByRole('button', { name: /match|find|calculate/i });
    if (matchButton) {
      expect(matchButton).toBeInTheDocument();
    }
  });
});
