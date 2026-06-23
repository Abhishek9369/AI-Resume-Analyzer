import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export interface ProcessTracker {
  id: string
  status: 'INITIATED' | 'EMBED_GENERATED' | 'VECTOR_DB_UPDATED' | 'RESUME_ANALYZED' | 'COMPLETED' | 'FAILED'
  totalFiles: number
  processedFiles: number
  failedFiles: number
  message?: string
  uploadedFilename?: string
  createdAt: string
  updatedAt: string
  completedAt?: string
  // Legacy fields for backward compatibility with API
  startTime?: string
  endTime?: string
}

interface UploadState {
  uploading: boolean
  tracker: ProcessTracker | null
  trackers: ProcessTracker[]
  fetchingTrackers: boolean
  error: string | null
}

const initialState: UploadState = {
  uploading: false,
  tracker: null,
  trackers: [],
  fetchingTrackers: false,
  error: null,
}

const uploadSlice = createSlice({
  name: 'upload',
  initialState,
  reducers: {
    uploadFiles: (state, _action: PayloadAction<File[]>) => {
      state.uploading = true
      state.error = null
    },
    uploadSuccess: (state, action: PayloadAction<ProcessTracker>) => {
      state.tracker = action.payload
      state.uploading = false
    },
    uploadFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.uploading = false
    },
    fetchProcessStatus: (_state, _action: PayloadAction<string>) => {
      // Initiates saga to fetch status
    },
    updateProcessStatus: (state, action: PayloadAction<ProcessTracker>) => {
      state.tracker = action.payload
    },
    clearTracker: (state) => {
      state.tracker = null
      state.error = null
    },
    fetchRecentTrackers: (state, _action: PayloadAction<number>) => {
      state.fetchingTrackers = true
    },
    fetchRecentTrackersSuccess: (state, action: PayloadAction<ProcessTracker[]>) => {
      state.trackers = action.payload
      state.fetchingTrackers = false
    },
    fetchRecentTrackersFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.fetchingTrackers = false
    },
  },
})

export const {
  uploadFiles,
  uploadSuccess,
  uploadFailure,
  fetchProcessStatus,
  updateProcessStatus,
  clearTracker,
  fetchRecentTrackers,
  fetchRecentTrackersSuccess,
  fetchRecentTrackersFailure,
} = uploadSlice.actions

export default uploadSlice.reducer
