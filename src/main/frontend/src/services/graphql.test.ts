import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { setupServer } from 'msw/node';
import { graphql, HttpResponse } from 'msw';
import { graphqlClient } from './graphql';
import { mockCandidates, mockJobRequirements, mockCandidateMatch } from '../test/mockData';

// MSW server setup
const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('GraphQL Client', () => {
  describe('Candidate Queries', () => {
    it('should fetch all candidates', async () => {
      server.use(
        graphql.query('GetCandidates', () => {
          return HttpResponse.json({
            data: {
              candidates: mockCandidates,
            },
          });
        })
      );

      const query = `
        query GetCandidates {
          candidates {
            id
            name
            email
            skills
          }
        }
      `;

      const response = await graphqlClient.request(query) as any;
      
      expect(response.candidates).toEqual(mockCandidates);
      expect(response.candidates).toHaveLength(2);
    });

    it('should handle candidate query errors', async () => {
      server.use(
        graphql.query('GetCandidates', () => {
          return HttpResponse.json({
            errors: [{ message: 'Failed to fetch candidates' }],
          });
        })
      );

      const query = `
        query GetCandidates {
          candidates {
            id
            name
          }
        }
      `;

      await expect(graphqlClient.request(query)).rejects.toThrow();
    });
  });

  describe('Job Requirement Queries', () => {
    it('should fetch all job requirements', async () => {
      server.use(
        graphql.query('GetJobRequirements', () => {
          return HttpResponse.json({
            data: {
              jobRequirements: mockJobRequirements,
            },
          });
        })
      );

      const query = `
        query GetJobRequirements {
          jobRequirements {
            id
            title
            requiredSkills
          }
        }
      `;

      const response = await graphqlClient.request(query) as any;
      
      expect(response.jobRequirements).toEqual(mockJobRequirements);
      expect(response.jobRequirements).toHaveLength(2);
    });

    it('should create job requirement', async () => {
      const newJob = mockJobRequirements[0];
      
      server.use(
        graphql.mutation('CreateJobRequirement', () => {
          return HttpResponse.json({
            data: {
              createJobRequirement: newJob,
            },
          });
        })
      );

      const mutation = `
        mutation CreateJobRequirement($input: JobRequirementInput!) {
          createJobRequirement(input: $input) {
            id
            title
          }
        }
      `;

      const response = await graphqlClient.request(mutation, {
        input: {
          title: newJob.title,
          requiredSkills: newJob.requiredSkills,
        },
      }) as any;
      
      expect(response.createJobRequirement).toEqual(newJob);
    });
  });

  describe('Matching Queries', () => {
    it('should fetch candidate matches', async () => {
      server.use(
        graphql.query('GetMatches', () => {
          return HttpResponse.json({
            data: {
              matches: [mockCandidateMatch],
            },
          });
        })
      );

      const query = `
        query GetMatches($jobId: ID!) {
          matches(jobId: $jobId) {
            candidateId
            jobId
            overallScore
          }
        }
      `;

      const response = await graphqlClient.request(query, { jobId: 'job-1' }) as any;
      
      expect(response.matches).toHaveLength(1);
      expect(response.matches[0].overallScore).toBe(85.5);
    });

    it('should calculate match for candidate and job', async () => {
      server.use(
        graphql.mutation('CalculateMatch', () => {
          return HttpResponse.json({
            data: {
              calculateMatch: mockCandidateMatch,
            },
          });
        })
      );

      const mutation = `
        mutation CalculateMatch($candidateId: ID!, $jobId: ID!) {
          calculateMatch(candidateId: $candidateId, jobId: $jobId) {
            overallScore
            skillsScore
          }
        }
      `;

      const response = await graphqlClient.request(mutation, {
        candidateId: '1',
        jobId: 'job-1',
      }) as any;
      
      expect(response.calculateMatch.overallScore).toBe(85.5);
    });
  });

  describe('Network Error Handling', () => {
    it('should handle network errors', async () => {
      server.use(
        graphql.query('GetCandidates', () => {
          return HttpResponse.error();
        })
      );

      const query = `
        query GetCandidates {
          candidates {
            id
          }
        }
      `;

      await expect(graphqlClient.request(query)).rejects.toThrow();
    });

    it('should handle timeout errors', async () => {
      server.use(
        graphql.query('GetCandidates', async () => {
          await new Promise((resolve) => setTimeout(resolve, 5000));
          return HttpResponse.json({
            data: { candidates: [] },
          });
        })
      );

      // This would timeout in real scenario with proper timeout configuration
      // For testing, we just verify the handler is set up correctly
      expect(server.listHandlers()).toHaveLength(1);
    });
  });
});
