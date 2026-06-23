/**
 * Authentication Redux Slice
 * Manages authentication state, user info, and tokens
 */

import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import type { AuthState, User, AuthResponse } from '@/types/auth'
import { getTokenExpiration } from '@/utils/tokenUtils'

// Initial state
const initialState: AuthState = {
  user: null,
  accessToken: localStorage.getItem('accessToken'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: false,
  isLoading: false,
  error: null,
  tokenExpiresAt: null,
}

// Initialize from localStorage if tokens exist
if (initialState.accessToken) {
  const storedUser = localStorage.getItem('user')
  if (storedUser && storedUser !== 'undefined' && storedUser !== 'null') {
    try {
      initialState.user = JSON.parse(storedUser)
      initialState.isAuthenticated = true
      initialState.tokenExpiresAt = getTokenExpiration(initialState.accessToken)
    } catch {
      // Corrupt data in localStorage — clear it
      localStorage.removeItem('user')
    }
  } else if (storedUser === 'undefined' || storedUser === 'null') {
    // Clean up invalid value written when user payload was undefined
    localStorage.removeItem('user')
  }
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    // Login actions
    loginRequest: (state, _action: PayloadAction<{ username: string; password: string }>) => {
      state.isLoading = true
      state.error = null
    },
    loginSuccess: (state, action: PayloadAction<AuthResponse>) => {
      state.isLoading = false
      state.isAuthenticated = true
      state.user = action.payload.user
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.tokenExpiresAt = getTokenExpiration(action.payload.accessToken)
      state.error = null

      // Persist to localStorage
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
      if (action.payload.user) {
        localStorage.setItem('user', JSON.stringify(action.payload.user))
      }
    },
    loginFailure: (state, action: PayloadAction<string>) => {
      state.isLoading = false
      state.isAuthenticated = false
      state.user = null
      state.accessToken = null
      state.refreshToken = null
      state.tokenExpiresAt = null
      state.error = action.payload
    },

    // Register actions
    registerRequest: (state) => {
      state.isLoading = true
      state.error = null
    },
    registerSuccess: (state, action: PayloadAction<AuthResponse>) => {
      state.isLoading = false
      state.isAuthenticated = true
      state.user = action.payload.user
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.tokenExpiresAt = getTokenExpiration(action.payload.accessToken)
      state.error = null

      // Persist to localStorage
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
      if (action.payload.user) {
        localStorage.setItem('user', JSON.stringify(action.payload.user))
      }
    },
    registerFailure: (state, action: PayloadAction<string>) => {
      state.isLoading = false
      state.error = action.payload
    },

    // Refresh token actions
    refreshTokenRequest: (state) => {
      state.error = null
    },
    refreshTokenSuccess: (state, action: PayloadAction<AuthResponse>) => {
      state.user = action.payload.user
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.tokenExpiresAt = getTokenExpiration(action.payload.accessToken)
      state.error = null

      // Update localStorage
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
      if (action.payload.user) {
        localStorage.setItem('user', JSON.stringify(action.payload.user))
      }
    },
    refreshTokenFailure: (state, action: PayloadAction<string>) => {
      state.isAuthenticated = false
      state.user = null
      state.accessToken = null
      state.refreshToken = null
      state.tokenExpiresAt = null
      state.error = action.payload

      // Clear localStorage
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    },

    // Get current user actions
    getCurrentUserRequest: (state) => {
      state.isLoading = true
      state.error = null
    },
    getCurrentUserSuccess: (state, action: PayloadAction<User>) => {
      state.isLoading = false
      state.user = action.payload
      state.isAuthenticated = true
      state.error = null

      // Update localStorage
      localStorage.setItem('user', JSON.stringify(action.payload))
    },
    getCurrentUserFailure: (state, action: PayloadAction<string>) => {
      state.isLoading = false
      state.error = action.payload
    },

    // Change password actions
    changePasswordRequest: (state) => {
      state.isLoading = true
      state.error = null
    },
    changePasswordSuccess: (state) => {
      state.isLoading = false
      state.error = null
    },
    changePasswordFailure: (state, action: PayloadAction<string>) => {
      state.isLoading = false
      state.error = action.payload
    },

    // Logout action
    logout: (state) => {
      state.isAuthenticated = false
      state.user = null
      state.accessToken = null
      state.refreshToken = null
      state.tokenExpiresAt = null
      state.isLoading = false
      state.error = null

      // Clear localStorage
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    },

    // Clear error
    clearError: (state) => {
      state.error = null
    },

    // Update user info (for profile updates)
    updateUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload
      localStorage.setItem('user', JSON.stringify(action.payload))
    },
  },
})

export const {
  loginRequest,
  loginSuccess,
  loginFailure,
  registerRequest,
  registerSuccess,
  registerFailure,
  refreshTokenRequest,
  refreshTokenSuccess,
  refreshTokenFailure,
  getCurrentUserRequest,
  getCurrentUserSuccess,
  getCurrentUserFailure,
  changePasswordRequest,
  changePasswordSuccess,
  changePasswordFailure,
  logout,
  clearError,
  updateUser,
} = authSlice.actions

export default authSlice.reducer
