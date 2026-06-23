import { describe, it, expect } from 'vitest';
import jobsReducer, {
  fetchJobs,
  fetchJobsSuccess,
  fetchJobsFailure,
  selectJob,
  createJobSuccess,
  updateJobSuccess,
  deleteJobSuccess,
} from './jobsSlice';
import { mockJobRequirements, mockJobRequirement } from '../../test/mockData';

describe('jobsSlice', () => {
  const initialState = {
    jobs: [],
    selectedJob: null,
    loading: false,
    error: null,
  };

  it('should return initial state', () => {
    expect(jobsReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  it('should handle fetchJobs', () => {
    const actual = jobsReducer(initialState, fetchJobs());
    
    expect(actual.loading).toBe(true);
    expect(actual.error).toBeNull();
  });

  it('should handle fetchJobsSuccess', () => {
    const actual = jobsReducer(initialState, fetchJobsSuccess(mockJobRequirements));
    
    expect(actual.jobs).toEqual(mockJobRequirements);
    expect(actual.jobs).toHaveLength(2);
    expect(actual.loading).toBe(false);
  });

  it('should handle fetchJobsFailure', () => {
    const errorMessage = 'Failed to load jobs';
    const actual = jobsReducer(initialState, fetchJobsFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.loading).toBe(false);
  });

  it('should handle selectJob', () => {
    const actual = jobsReducer(initialState, selectJob(mockJobRequirement));
    
    expect(actual.selectedJob).toEqual(mockJobRequirement);
  });

  it('should handle createJobSuccess', () => {
    const actual = jobsReducer(initialState, createJobSuccess(mockJobRequirement));
    
    expect(actual.jobs).toHaveLength(1);
    expect(actual.jobs[0]).toEqual(mockJobRequirement);
    expect(actual.loading).toBe(false);
  });

  it('should handle updateJobSuccess', () => {
    const stateWithJobs = {
      ...initialState,
      jobs: [mockJobRequirement],
    };
    
    const updatedJob = {
      ...mockJobRequirement,
      title: 'Updated Senior Java Developer',
    };
    
    const actual = jobsReducer(stateWithJobs, updateJobSuccess(updatedJob));
    
    expect(actual.jobs[0].title).toBe('Updated Senior Java Developer');
    expect(actual.loading).toBe(false);
  });

  it('should handle deleteJobSuccess', () => {
    const stateWithJobs = {
      ...initialState,
      jobs: mockJobRequirements,
    };
    
    const actual = jobsReducer(stateWithJobs, deleteJobSuccess(mockJobRequirements[0].id));
    
    expect(actual.jobs).toHaveLength(1);
    expect(actual.jobs.find(j => j.id === mockJobRequirements[0].id)).toBeUndefined();
  });

  it('should clear selected job on delete', () => {
    const stateWithSelected = {
      ...initialState,
      jobs: [mockJobRequirement],
      selectedJob: mockJobRequirement,
    };
    
    const actual = jobsReducer(stateWithSelected, deleteJobSuccess(mockJobRequirement.id));
    
    expect(actual.selectedJob).toBeNull();
  });

  it('should update selected job when updating it', () => {
    const stateWithSelected = {
      ...initialState,
      jobs: [mockJobRequirement],
      selectedJob: mockJobRequirement,
    };
    
    const updatedJob = {
      ...mockJobRequirement,
      title: 'Updated Title',
    };
    
    const actual = jobsReducer(stateWithSelected, updateJobSuccess(updatedJob));
    
    expect(actual.selectedJob?.title).toBe('Updated Title');
  });
});
