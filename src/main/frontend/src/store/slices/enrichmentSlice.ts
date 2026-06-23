import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export type ExternalProfileSource = 'GITHUB' | 'LINKEDIN' | 'TWITTER' | 'INTERNET_SEARCH'
export type EnrichmentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'NOT_FOUND' | 'NOT_AVAILABLE'

export interface CandidateExternalProfile {
  id: string
  source: ExternalProfileSource
  profileUrl: string | null
  displayName: string | null
  bio: string | null
  location: string | null
  company: string | null
  publicRepos: number | null
  followers: number | null
  repositories: string | null
  enrichedSummary: string | null
  status: EnrichmentStatus
  errorMessage: string | null
  lastFetchedAt: string | null
  createdAt: string
  updatedAt: string
}

interface EnrichmentState {
  // Map of candidateId -> list of external profiles
  profilesByCandidateId: Record<string, CandidateExternalProfile[]>
  loadingCandidateId: string | null
  enrichingCandidateId: string | null
  error: string | null
}

const initialState: EnrichmentState = {
  profilesByCandidateId: {},
  loadingCandidateId: null,
  enrichingCandidateId: null,
  error: null,
}

const enrichmentSlice = createSlice({
  name: 'enrichment',
  initialState,
  reducers: {
    fetchExternalProfiles: (state, action: PayloadAction<string>) => {
      state.loadingCandidateId = action.payload
      state.error = null
    },
    fetchExternalProfilesSuccess: (
      state,
      action: PayloadAction<{ candidateId: string; profiles: CandidateExternalProfile[] }>
    ) => {
      state.profilesByCandidateId[action.payload.candidateId] = action.payload.profiles
      state.loadingCandidateId = null
    },
    fetchExternalProfilesFailure: (state, action: PayloadAction<string>) => {
      state.loadingCandidateId = null
      state.error = action.payload
    },
    enrichProfile: (
      state,
      _action: PayloadAction<{ candidateId: string; source: ExternalProfileSource }>
    ) => {
      state.enrichingCandidateId = _action.payload.candidateId
      state.error = null
    },
    enrichProfileSuccess: (
      state,
      action: PayloadAction<{ candidateId: string; profile: CandidateExternalProfile }>
    ) => {
      const { candidateId, profile } = action.payload
      const existing = state.profilesByCandidateId[candidateId] ?? []
      const idx = existing.findIndex((p) => p.id === profile.id)
      if (idx !== -1) {
        existing[idx] = profile
      } else {
        existing.push(profile)
      }
      state.profilesByCandidateId[candidateId] = existing
      state.enrichingCandidateId = null
    },
    enrichProfileFailure: (state, action: PayloadAction<string>) => {
      state.enrichingCandidateId = null
      state.error = action.payload
    },
    refreshProfile: (
      state,
      _action: PayloadAction<{ profileId: string; candidateId: string }>
    ) => {
      state.enrichingCandidateId = _action.payload.candidateId
      state.error = null
    },
    refreshProfileSuccess: (
      state,
      action: PayloadAction<{ candidateId: string; profile: CandidateExternalProfile }>
    ) => {
      const { candidateId, profile } = action.payload
      const existing = state.profilesByCandidateId[candidateId] ?? []
      const idx = existing.findIndex((p) => p.id === profile.id)
      if (idx !== -1) {
        existing[idx] = profile
      } else {
        existing.push(profile)
      }
      state.profilesByCandidateId[candidateId] = existing
      state.enrichingCandidateId = null
    },
    refreshProfileFailure: (state, action: PayloadAction<string>) => {
      state.enrichingCandidateId = null
      state.error = action.payload
    },
    enrichFromUrl: (
      state,
      _action: PayloadAction<{ candidateId: string; profileUrl: string }>
    ) => {
      state.enrichingCandidateId = _action.payload.candidateId
      state.error = null
    },
    enrichFromUrlSuccess: (
      state,
      action: PayloadAction<{ candidateId: string; profile: CandidateExternalProfile }>
    ) => {
      const { candidateId, profile } = action.payload
      const existing = state.profilesByCandidateId[candidateId] ?? []
      const idx = existing.findIndex((p) => p.id === profile.id)
      if (idx !== -1) {
        existing[idx] = profile
      } else {
        existing.push(profile)
      }
      state.profilesByCandidateId[candidateId] = existing
      state.enrichingCandidateId = null
    },
    enrichFromUrlFailure: (state, action: PayloadAction<string>) => {
      state.enrichingCandidateId = null
      state.error = action.payload
    },
  },
})

export const {
  fetchExternalProfiles,
  fetchExternalProfilesSuccess,
  fetchExternalProfilesFailure,
  enrichProfile,
  enrichProfileSuccess,
  enrichProfileFailure,
  refreshProfile,
  refreshProfileSuccess,
  refreshProfileFailure,
  enrichFromUrl,
  enrichFromUrlSuccess,
  enrichFromUrlFailure,
} = enrichmentSlice.actions

export default enrichmentSlice.reducer
