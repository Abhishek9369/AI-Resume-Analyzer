import { describe, it, expect } from 'vitest';
import matchesReducer, {
  fetchMatchesForJob,
  fetchMatchesSuccess,
  fetchMatchesFailure,
  matchCandidateToJob,
  matchingSuccess,
  matchingFailure,
  updateMatchStatusSuccess,
} from './matchesSlice';
import { mockCandidateMatch } from '../../test/mockData';

describe('matchesSlice', () => {
  const initialState = {
    matches: [],
    loading: false,
    error: null,
    matchingInProgress: false,
  };

  it('should return initial state', () => {
    expect(matchesReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  it('should handle fetchMatchesForJob', () => {
    const actual = matchesReducer(initialState, fetchMatchesForJob({ jobId: '1' }));
    
    expect(actual.loading).toBe(true);
    expect(actual.error).toBeNull();
  });

  it('should handle fetchMatchesSuccess', () => {
    const matches = [mockCandidateMatch];
    const actual = matchesReducer(initialState, fetchMatchesSuccess(matches));
    
    expect(actual.matches).toEqual(matches);
    expect(actual.matches).toHaveLength(1);
    expect(actual.loading).toBe(false);
  });

  it('should handle fetchMatchesFailure', () => {
    const errorMessage = 'Failed to fetch matches';
    const actual = matchesReducer(initialState, fetchMatchesFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.loading).toBe(false);
  });

  it('should handle matchCandidateToJob', () => {
    const actual = matchesReducer(
      initialState,
      matchCandidateToJob({ candidateId: '1', jobId: '1' })
    );
    
    expect(actual.matchingInProgress).toBe(true);
  });

  it('should handle matchingSuccess with single match', () => {
    const actual = matchesReducer(initialState, matchingSuccess(mockCandidateMatch));
    
    expect(actual.matches).toHaveLength(1);
    expect(actual.matches[0]).toEqual(mockCandidateMatch);
    expect(actual.matchingInProgress).toBe(false);
  });

  it('should handle matchingSuccess with array of matches', () => {
    const matches = [mockCandidateMatch, { ...mockCandidateMatch, id: '2' }];
    const actual = matchesReducer(initialState, matchingSuccess(matches));
    
    expect(actual.matches).toHaveLength(2);
    expect(actual.matchingInProgress).toBe(false);
  });

  it('should update existing match on matchingSuccess', () => {
    const stateWithMatch = {
      ...initialState,
      matches: [mockCandidateMatch],
    };
    
    const updatedMatch = { ...mockCandidateMatch, matchScore: 95.5 };
    const actual = matchesReducer(stateWithMatch, matchingSuccess(updatedMatch));
    
    expect(actual.matches).toHaveLength(1);
    expect(actual.matches[0].matchScore).toBe(95.5);
  });

  it('should handle matchingFailure', () => {
    const stateWithMatching = {
      ...initialState,
      matchingInProgress: true,
    };
    
    const errorMessage = 'Failed to calculate match';
    const actual = matchesReducer(stateWithMatching, matchingFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.matchingInProgress).toBe(false);
  });

  it('should handle updateMatchStatusSuccess', () => {
    const stateWithMatch = {
      ...initialState,
      matches: [mockCandidateMatch],
    };
    
    const updatedMatch = { ...mockCandidateMatch, isShortlisted: true };
    const actual = matchesReducer(stateWithMatch, updateMatchStatusSuccess(updatedMatch));
    
    expect(actual.matches[0].isShortlisted).toBe(true);
    expect(actual.loading).toBe(false);
  });
});
