import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export interface Skill {
  id: string
  name: string
  category?: string
}

export interface JobRequirement {
  id: string
  title: string
  requiredSkills?: string
  skills?: Skill[]
  minExperienceYears: number
  maxExperienceYears: number
  requiredEducation?: string
  domainRequirements?: string
  description?: string
  isActive: boolean
  createdAt: string
}

interface JobsState {
  jobs: JobRequirement[]
  selectedJob: JobRequirement | null
  loading: boolean
  error: string | null
}

const initialState: JobsState = {
  jobs: [],
  selectedJob: null,
  loading: false,
  error: null,
}

const jobsSlice = createSlice({
  name: 'jobs',
  initialState,
  reducers: {
    fetchJobs: (state) => {
      state.loading = true
      state.error = null
    },
    fetchJobsSuccess: (state, action: PayloadAction<JobRequirement[]>) => {
      state.jobs = action.payload
      state.loading = false
    },
    fetchJobsFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.loading = false
    },
    selectJob: (state, action: PayloadAction<JobRequirement | null>) => {
      state.selectedJob = action.payload
    },
    createJob: (state, _action: PayloadAction<Omit<JobRequirement, 'id' | 'createdAt'>>) => {
      state.loading = true
    },
    createJobSuccess: (state, action: PayloadAction<JobRequirement>) => {
      state.jobs.push(action.payload)
      state.loading = false
    },
    updateJob: (state, _action: PayloadAction<JobRequirement>) => {
      state.loading = true
    },
    updateJobSuccess: (state, action: PayloadAction<JobRequirement>) => {
      const index = state.jobs.findIndex((j) => j.id === action.payload.id)
      if (index !== -1) {
        state.jobs[index] = action.payload
      }
      if (state.selectedJob?.id === action.payload.id) {
        state.selectedJob = action.payload
      }
      state.loading = false
    },
    deleteJob: (state, _action: PayloadAction<string>) => {
      state.loading = true
    },
    deleteJobSuccess: (state, action: PayloadAction<string>) => {
      state.jobs = state.jobs.filter((j) => j.id !== action.payload)
      if (state.selectedJob?.id === action.payload) {
        state.selectedJob = null
      }
      state.loading = false
    },
  },
})

export const {
  fetchJobs,
  fetchJobsSuccess,
  fetchJobsFailure,
  selectJob,
  createJob,
  createJobSuccess,
  updateJob,
  updateJobSuccess,
  deleteJob,
  deleteJobSuccess,
} = jobsSlice.actions

export default jobsSlice.reducer
