/**
 * Authentication Redux Selectors
 * Memoized selectors for accessing auth state
 */

import { createSelector } from '@reduxjs/toolkit'
import type { RootState } from '@store/index'
import { UserRole } from '@/types/auth'

// Base selectors
export const selectAuth = (state: RootState) => state.auth

export const selectUser = (state: RootState) => state.auth.user
export const selectIsAuthenticated = (state: RootState) => state.auth.isAuthenticated
export const selectIsLoading = (state: RootState) => state.auth.isLoading
export const selectAuthError = (state: RootState) => state.auth.error
export const selectAccessToken = (state: RootState) => state.auth.accessToken
export const selectTokenExpiresAt = (state: RootState) => state.auth.tokenExpiresAt

// Derived selectors
export const selectUserRole = createSelector(
  [selectUser],
  (user) => user?.role || null
)

export const selectUsername = createSelector(
  [selectUser],
  (user) => user?.username || null
)

export const selectUserFullName = createSelector(
  [selectUser],
  (user) => user?.fullName || null
)

export const selectUserEmail = createSelector(
  [selectUser],
  (user) => user?.email || null
)

export const selectIsActive = createSelector(
  [selectUser],
  (user) => user?.isActive || false
)

// Role-based selectors
export const selectIsAdmin = createSelector(
  [selectUserRole],
  (role) => role === UserRole.ADMIN
)

export const selectIsRecruiter = createSelector(
  [selectUserRole],
  (role) => role === UserRole.RECRUITER
)

export const selectIsHR = createSelector(
  [selectUserRole],
  (role) => role === UserRole.HR
)

export const selectIsHiringManager = createSelector(
  [selectUserRole],
  (role) => role === UserRole.HIRING_MANAGER
)

export const selectCanManageUsers = createSelector(
  [selectIsAdmin],
  (isAdmin) => isAdmin
)

export const selectCanManageJobs = createSelector(
  [selectUserRole],
  (role) => role === UserRole.ADMIN || role === UserRole.RECRUITER
)

export const selectCanUploadResumes = createSelector(
  [selectUserRole],
  (role) => role === UserRole.ADMIN || role === UserRole.RECRUITER
)

export const selectCanManageEmployees = createSelector(
  [selectUserRole],
  (role) => role === UserRole.ADMIN || role === UserRole.HR
)

export const selectCanViewCandidates = createSelector(
  [selectUserRole],
  (role) =>
    role === UserRole.ADMIN ||
    role === UserRole.RECRUITER ||
    role === UserRole.HIRING_MANAGER ||
    role === UserRole.HR
)

export const selectCanProvideFeedback = createSelector(
  [selectUserRole],
  (role) =>
    role === UserRole.ADMIN ||
    role === UserRole.RECRUITER ||
    role === UserRole.HIRING_MANAGER
)

export const selectCanShortlistCandidates = createSelector(
  [selectUserRole],
  (role) => role === UserRole.ADMIN || role === UserRole.RECRUITER
)

// Token expiration selector
export const selectIsTokenExpiringSoon = createSelector(
  [selectTokenExpiresAt],
  (expiresAt) => {
    if (!expiresAt) return false
    const now = Date.now()
    const fiveMinutes = 5 * 60 * 1000
    return expiresAt - now < fiveMinutes
  }
)

export const selectIsTokenExpired = createSelector(
  [selectTokenExpiresAt],
  (expiresAt) => {
    if (!expiresAt) return true
    return Date.now() > expiresAt
  }
)
