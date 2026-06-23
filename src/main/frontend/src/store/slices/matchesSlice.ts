import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export interface CandidateMatch {
  id: string
  candidateId: string
  jobRequirementId: string
  matchScore: number
  skillsScore: number
  experienceScore: number
  educationScore: number
  domainScore: number
  explanation?: string
  isShortlisted: boolean
  isSelected: boolean
  createdAt: string
}

interface MatchesState {
  matches: CandidateMatch[]
  loading: boolean
  error: string | null
  matchingInProgress: boolean
}

const initialState: MatchesState = {
  matches: [],
  loading: false,
  error: null,
  matchingInProgress: false,
}

const matchesSlice = createSlice({
  name: 'matches',
  initialState,
  reducers: {
    fetchMatchesForJob: (state, _action: PayloadAction<{ jobId: string; limit?: number }>) => {
      state.loading = true
      state.error = null
    },
    fetchMatchesSuccess: (state, action: PayloadAction<CandidateMatch[]>) => {
      state.matches = action.payload
      state.loading = false
    },
    fetchMatchesFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.loading = false
    },
    matchCandidateToJob: (
      state,
      _action: PayloadAction<{ candidateId: string; jobId: string }>
    ) => {
      state.matchingInProgress = true
    },
    matchAllCandidatesToJob: (state, _action: PayloadAction<string>) => {
      state.matchingInProgress = true
    },
    matchCandidateToAllJobs: (state, _action: PayloadAction<string>) => {
      state.matchingInProgress = true
    },
    matchingSuccess: (state, action: PayloadAction<CandidateMatch | CandidateMatch[]>) => {
      const matches = Array.isArray(action.payload) ? action.payload : [action.payload]
      matches.forEach((match) => {
        const index = state.matches.findIndex((m) => m.id === match.id)
        if (index !== -1) {
          state.matches[index] = match
        } else {
          state.matches.push(match)
        }
      })
      state.matchingInProgress = false
    },
    matchingFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.matchingInProgress = false
    },
    updateMatchStatus: (
      state,
      _action: PayloadAction<{ matchId: string; isShortlisted?: boolean; isSelected?: boolean }>
    ) => {
      state.loading = true
    },
    updateMatchStatusSuccess: (state, action: PayloadAction<CandidateMatch>) => {
      const index = state.matches.findIndex((m) => m.id === action.payload.id)
      if (index !== -1) {
        state.matches[index] = action.payload
      }
      state.loading = false
    },
  },
})

export const {
  fetchMatchesForJob,
  fetchMatchesSuccess,
  fetchMatchesFailure,
  matchCandidateToJob,
  matchAllCandidatesToJob,
  matchCandidateToAllJobs,
  matchingSuccess,
  matchingFailure,
  updateMatchStatus,
  updateMatchStatusSuccess,
} = matchesSlice.actions

export default matchesSlice.reducer
