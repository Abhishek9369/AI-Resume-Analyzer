import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export interface Candidate {
  id: string
  name: string
  email: string
  mobile?: string
  skills: string
  yearsOfExperience?: number
  academicBackground?: string
  experienceSummary?: string
  createdAt: string
}

interface CandidatesState {
  candidates: Candidate[]
  selectedCandidate: Candidate | null
  loading: boolean
  error: string | null
  searchQuery: string
}

const initialState: CandidatesState = {
  candidates: [],
  selectedCandidate: null,
  loading: false,
  error: null,
  searchQuery: '',
}

const candidatesSlice = createSlice({
  name: 'candidates',
  initialState,
  reducers: {
    fetchCandidates: (state) => {
      state.loading = true
      state.error = null
    },
    fetchCandidatesSuccess: (state, action: PayloadAction<Candidate[]>) => {
      state.candidates = action.payload
      state.loading = false
    },
    fetchCandidatesFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload
      state.loading = false
    },
    searchCandidatesByName: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload
      state.loading = true
    },
    searchCandidatesBySkill: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload
      state.loading = true
    },
    selectCandidate: (state, action: PayloadAction<Candidate | null>) => {
      state.selectedCandidate = action.payload
    },
    updateCandidate: (state, _action: PayloadAction<Candidate>) => {
      state.loading = true
    },
    updateCandidateSuccess: (state, action: PayloadAction<Candidate>) => {
      const index = state.candidates.findIndex((c) => c.id === action.payload.id)
      if (index !== -1) {
        state.candidates[index] = action.payload
      }
      if (state.selectedCandidate?.id === action.payload.id) {
        state.selectedCandidate = action.payload
      }
      state.loading = false
    },
    deleteCandidate: (state, _action: PayloadAction<string>) => {
      state.loading = true
    },
    deleteCandidateSuccess: (state, action: PayloadAction<string>) => {
      state.candidates = state.candidates.filter((c) => c.id !== action.payload)
      if (state.selectedCandidate?.id === action.payload) {
        state.selectedCandidate = null
      }
      state.loading = false
    },
  },
})

export const {
  fetchCandidates,
  fetchCandidatesSuccess,
  fetchCandidatesFailure,
  searchCandidatesByName,
  searchCandidatesBySkill,
  selectCandidate,
  updateCandidate,
  updateCandidateSuccess,
  deleteCandidate,
  deleteCandidateSuccess,
} = candidatesSlice.actions

export default candidatesSlice.reducer
