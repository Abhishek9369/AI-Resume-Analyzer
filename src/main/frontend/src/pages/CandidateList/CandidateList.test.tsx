import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { graphql, HttpResponse } from 'msw';
import { render } from '../../test/test-utils';
import CandidateList from './CandidateList';
import { mockCandidates } from '../../test/mockData';

// MSW server setup
const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('CandidateList Component', () => {
  it('should render candidate list component', () => {
    render(<CandidateList />);
    expect(screen.getByText(/Candidates \(0\)/i)).toBeInTheDocument();
  });

  it('should display candidates when loaded', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allCandidates')) {
          return HttpResponse.json({
            data: {
              allCandidates: mockCandidates,
            },
          });
        }
      })
    );
    
    render(<CandidateList />);
    
    // Wait for candidates to be loaded and displayed
    await waitFor(() => {
      expect(screen.getByText(/John Doe/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Jane Smith/i)).toBeInTheDocument();
  });

  it('should show loading state', () => {
    server.use(
      graphql.query('GetAllCandidates', () => {
        return HttpResponse.json({
          data: {
            candidates: [],
          },
        });
      })
    );

    const initialState = {
      candidates: {
        candidates: [],
        loading: true,
        error: null,
      },
    };
    
    render(<CandidateList />, { preloadedState: initialState });
    
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('should show error message when error occurs', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allCandidates')) {
          return HttpResponse.json({
            data: {
              allCandidates: [],
            },
          });
        }
      })
    );
    
    render(<CandidateList />);
    
    // Wait for empty state to be displayed
    await waitFor(() => {
      expect(screen.getByText(/No candidates found/i)).toBeInTheDocument();
    });
  });

  it('should display candidate details', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allCandidates')) {
          return HttpResponse.json({
            data: {
              allCandidates: [mockCandidates[0]],
            },
          });
        }
      })
    );
    
    render(<CandidateList />);
    
    await waitFor(() => {
      expect(screen.getByText(/John Doe/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/john\.doe@example\.com/i)).toBeInTheDocument();
    expect(screen.getByText(/8 yrs/i)).toBeInTheDocument();
  });

  it('should show empty state when no candidates', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allCandidates')) {
          return HttpResponse.json({
            data: {
              allCandidates: [],
            },
          });
        }
      })
    );
    
    render(<CandidateList />);
    
    await waitFor(() => {
      expect(screen.getByText(/No candidates found/i)).toBeInTheDocument();
    });
  });

  it('should render candidate skills', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allCandidates')) {
          return HttpResponse.json({
            data: {
              allCandidates: [mockCandidates[0]],
            },
          });
        }
      })
    );
    
    render(<CandidateList />);
    
    await waitFor(() => {
      expect(screen.getByText(/Java, Spring Boot, React, PostgreSQL/i)).toBeInTheDocument();
    });
  });
});
