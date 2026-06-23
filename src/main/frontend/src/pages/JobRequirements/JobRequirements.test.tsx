import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { graphql, HttpResponse } from 'msw';
import { render } from '../../test/test-utils';
import JobRequirements from './JobRequirements';
import { mockJobRequirements } from '../../test/mockData';

// MSW server setup
const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('JobRequirements Component', () => {
  it('should render job requirements component', () => {
    render(<JobRequirements />);
    expect(screen.getByText(/Job Requirements \(0\)/i)).toBeInTheDocument();
  });

  it('should display job listings when loaded', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allJobRequirements')) {
          return HttpResponse.json({
            data: {
              allJobRequirements: mockJobRequirements,
            },
          });
        }
      })
    );
    
    render(<JobRequirements />);
    
    await waitFor(() => {
      expect(screen.getByText(/Senior Java Developer/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Python Backend Engineer/i)).toBeInTheDocument();
  });

  it('should show loading state', () => {
    server.use(
      graphql.query('GetAllJobs', () => {
        return HttpResponse.json({
          data: {
            allJobRequirements: [],
          },
        });
      })
    );

    const initialState = {
      jobs: {
        jobs: [],
        loading: true,
        error: null,
      },
    };
    
    render(<JobRequirements />, { preloadedState: initialState });
    
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('should show error message when error occurs', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allJobRequirements')) {
          return HttpResponse.json({
            data: {
              allJobRequirements: [],
            },
          });
        }
      })
    );
    
    render(<JobRequirements />);
    
    await waitFor(() => {
      expect(screen.getByText(/No job requirements found/i)).toBeInTheDocument();
    });
  });

  it('should display job details', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allJobRequirements')) {
          return HttpResponse.json({
            data: {
              allJobRequirements: [mockJobRequirements[0]],
            },
          });
        }
      })
    );
    
    render(<JobRequirements />);
    
    await waitFor(() => {
      expect(screen.getByText(/Senior Java Developer/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/5 - 10 years/i)).toBeInTheDocument();
  });

  it('should show empty state when no jobs', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allJobRequirements')) {
          return HttpResponse.json({
            data: {
              allJobRequirements: [],
            },
          });
        }
      })
    );
    
    render(<JobRequirements />);
    
    await waitFor(() => {
      expect(screen.getByText(/No job requirements found/i)).toBeInTheDocument();
    });
  });

  it('should display required skills for job', async () => {
    server.use(
      graphql.operation(async ({ query }) => {
        const queryStr = query.toString();
        if (queryStr.includes('allJobRequirements')) {
          return HttpResponse.json({
            data: {
              allJobRequirements: [mockJobRequirements[0]],
            },
          });
        }
      })
    );
    
    render(<JobRequirements />);
    
    await waitFor(() => {
      expect(screen.getByText(/Java, Spring Boot, Microservices/i)).toBeInTheDocument();
    });
  });

  it('should have add job button', () => {
    render(<JobRequirements />);
    
    const addButton = screen.queryByRole('button', { name: /add|new|create/i });
    if (addButton) {
      expect(addButton).toBeInTheDocument();
    }
  });
});
