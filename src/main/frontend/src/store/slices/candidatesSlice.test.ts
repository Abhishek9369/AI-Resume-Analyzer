import { describe, it, expect } from 'vitest';
import candidatesReducer, {
  fetchCandidates,
  fetchCandidatesSuccess,
  fetchCandidatesFailure,
  selectCandidate,
  updateCandidateSuccess,
  deleteCandidateSuccess,
} from './candidatesSlice';
import { mockCandidates, mockCandidate } from '../../test/mockData';

describe('candidatesSlice', () => {
  const initialState = {
    candidates: [],
    selectedCandidate: null,
    loading: false,
    error: null,
    searchQuery: '',
  };

  it('should return initial state', () => {
    expect(candidatesReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  it('should handle fetchCandidates', () => {
    const actual = candidatesReducer(initialState, fetchCandidates());
    
    expect(actual.loading).toBe(true);
    expect(actual.error).toBeNull();
  });

  it('should handle fetchCandidatesSuccess', () => {
    const actual = candidatesReducer(initialState, fetchCandidatesSuccess(mockCandidates));
    
    expect(actual.candidates).toEqual(mockCandidates);
    expect(actual.candidates).toHaveLength(2);
    expect(actual.loading).toBe(false);
  });

  it('should handle fetchCandidatesFailure', () => {
    const errorMessage = 'Failed to load candidates';
    const actual = candidatesReducer(initialState, fetchCandidatesFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.loading).toBe(false);
  });

  it('should handle selectCandidate', () => {
    const actual = candidatesReducer(initialState, selectCandidate(mockCandidate));
    
    expect(actual.selectedCandidate).toEqual(mockCandidate);
  });

  it('should handle updateCandidateSuccess', () => {
    const stateWithCandidates = {
      ...initialState,
      candidates: [mockCandidate],
    };
    
    const updatedCandidate = {
      ...mockCandidate,
      name: 'John Updated',
    };
    
    const actual = candidatesReducer(stateWithCandidates, updateCandidateSuccess(updatedCandidate));
    
    expect(actual.candidates[0].name).toBe('John Updated');
    expect(actual.loading).toBe(false);
  });

  it('should handle deleteCandidateSuccess', () => {
    const stateWithCandidates = {
      ...initialState,
      candidates: mockCandidates,
    };
    
    const actual = candidatesReducer(stateWithCandidates, deleteCandidateSuccess(mockCandidates[0].id));
    
    expect(actual.candidates).toHaveLength(1);
    expect(actual.candidates.find(c => c.id === mockCandidates[0].id)).toBeUndefined();
  });

  it('should clear selected candidate on delete', () => {
    const stateWithSelected = {
      ...initialState,
      candidates: [mockCandidate],
      selectedCandidate: mockCandidate,
    };
    
    const actual = candidatesReducer(stateWithSelected, deleteCandidateSuccess(mockCandidate.id));
    
    expect(actual.selectedCandidate).toBeNull();
  });
});
