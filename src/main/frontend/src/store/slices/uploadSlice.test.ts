import { describe, it, expect } from 'vitest';
import uploadReducer, {
  uploadFiles,
  uploadSuccess,
  uploadFailure,
  updateProcessStatus,
  clearTracker,
  fetchRecentTrackers,
  fetchRecentTrackersSuccess,
  fetchRecentTrackersFailure,
} from './uploadSlice';
import { mockProcessTracker } from '../../test/mockData';

describe('uploadSlice', () => {
  const initialState = {
    uploading: false,
    tracker: null,
    trackers: [],
    fetchingTrackers: false,
    error: null,
  };

  it('should return initial state', () => {
    expect(uploadReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  it('should handle uploadFiles', () => {
    const files = [new File(['content'], 'resume.pdf', { type: 'application/pdf' })];
    const actual = uploadReducer(initialState, uploadFiles(files));
    
    expect(actual.uploading).toBe(true);
    expect(actual.error).toBeNull();
  });

  it('should handle uploadSuccess', () => {
    const uploadingState = { ...initialState, uploading: true };
    const actual = uploadReducer(uploadingState, uploadSuccess(mockProcessTracker));
    
    expect(actual.uploading).toBe(false);
    expect(actual.tracker).toEqual(mockProcessTracker);
    expect(actual.tracker?.status).toBe('COMPLETED');
  });

  it('should handle uploadFailure', () => {
    const uploadingState = { ...initialState, uploading: true };
    const errorMessage = 'Upload failed';
    const actual = uploadReducer(uploadingState, uploadFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.uploading).toBe(false);
  });

  it('should handle updateProcessStatus', () => {
    const updatedTracker = {
      ...mockProcessTracker,
      status: 'EMBED_GENERATED' as const,
      processedFiles: 5,
    };
    
    const actual = uploadReducer(initialState, updateProcessStatus(updatedTracker));
    
    expect(actual.tracker).toEqual(updatedTracker);
    expect(actual.tracker?.status).toBe('EMBED_GENERATED');
  });

  it('should handle clearTracker', () => {
    const stateWithTracker = {
      ...initialState,
      tracker: mockProcessTracker,
      error: 'Some error',
    };
    
    const actual = uploadReducer(stateWithTracker, clearTracker());
    
    expect(actual.tracker).toBeNull();
    expect(actual.error).toBeNull();
  });

  it('should handle fetchRecentTrackers', () => {
    const actual = uploadReducer(initialState, fetchRecentTrackers(10));
    
    expect(actual.fetchingTrackers).toBe(true);
  });

  it('should handle fetchRecentTrackersSuccess', () => {
    const fetchingState = { ...initialState, fetchingTrackers: true };
    const trackers = [mockProcessTracker];
    
    const actual = uploadReducer(fetchingState, fetchRecentTrackersSuccess(trackers));
    
    expect(actual.trackers).toEqual(trackers);
    expect(actual.fetchingTrackers).toBe(false);
  });

  it('should handle fetchRecentTrackersFailure', () => {
    const fetchingState = { ...initialState, fetchingTrackers: true };
    const errorMessage = 'Failed to fetch trackers';
    
    const actual = uploadReducer(fetchingState, fetchRecentTrackersFailure(errorMessage));
    
    expect(actual.error).toBe(errorMessage);
    expect(actual.fetchingTrackers).toBe(false);
  });

  it('should handle full upload lifecycle', () => {
    const files = [new File(['content'], 'resume.pdf', { type: 'application/pdf' })];
    
    // Start upload
    let state = uploadReducer(initialState, uploadFiles(files));
    expect(state.uploading).toBe(true);
    
    // Upload success with tracker
    const tracker = { ...mockProcessTracker, status: 'INITIATED' as const };
    state = uploadReducer(state, uploadSuccess(tracker));
    expect(state.uploading).toBe(false);
    expect(state.tracker).toEqual(tracker);
    
    // Status updates
    state = uploadReducer(state, updateProcessStatus({
      ...tracker,
      status: 'EMBED_GENERATED' as const,
    }));
    expect(state.tracker?.status).toBe('EMBED_GENERATED');
    
    state = uploadReducer(state, updateProcessStatus({
      ...tracker,
      status: 'COMPLETED' as const,
    }));
    expect(state.tracker?.status).toBe('COMPLETED');
    
    // Clear for next upload
    state = uploadReducer(state, clearTracker());
    expect(state.tracker).toBeNull();
  });

  it('should clear error when starting new upload', () => {
    const stateWithError = { ...initialState, error: 'Previous error' };
    const files = [new File(['content'], 'resume.pdf', { type: 'application/pdf' })];
    const actual = uploadReducer(stateWithError, uploadFiles(files));
    
    expect(actual.error).toBeNull();
    expect(actual.uploading).toBe(true);
  });
});
