/**
 * Authentication Redux Sagas
 * Handles async authentication operations
 */

import { call, put, takeLatest } from 'redux-saga/effects'
import { PayloadAction } from '@reduxjs/toolkit'
import { authApi } from '@/services/authApi'
import { setAuthToken, clearAuthTokens } from '@/services/axiosInstance'
import * as authActions from '@store/slices/authSlice'
import type {
  AuthResponse,
  LoginCredentials,
  RegisterData,
  ChangePasswordData,
  User,
} from '@/types/auth'

/**
 * Login saga
 */
function* loginSaga(action: PayloadAction<LoginCredentials>) {
  try {
    const response: AuthResponse = yield call(authApi.login, action.payload)

    // Set auth token in axios headers
    yield call(setAuthToken, response.accessToken)

    yield put(authActions.loginSuccess(response))
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : 'Login failed'
    yield put(authActions.loginFailure(errorMessage))
  }
}

/**
 * Register saga
 */
function* registerSaga(action: PayloadAction<RegisterData>) {
  try {
    const response: AuthResponse = yield call(authApi.register, action.payload)

    // Set auth token in axios headers
    yield call(setAuthToken, response.accessToken)

    yield put(authActions.registerSuccess(response))
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : 'Registration failed'
    yield put(authActions.registerFailure(errorMessage))
  }
}

/**
 * Refresh token saga
 */
function* refreshTokenSaga() {
  try {
    const refreshToken = localStorage.getItem('refreshToken')

    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    const response: AuthResponse = yield call(
      authApi.refreshToken,
      refreshToken
    )

    // Set new auth token in axios headers
    yield call(setAuthToken, response.accessToken)

    yield put(authActions.refreshTokenSuccess(response))
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : 'Token refresh failed'
    yield put(authActions.refreshTokenFailure(errorMessage))

    // Clear tokens on refresh failure
    yield call(clearAuthTokens)
  }
}

/**
 * Get current user saga
 */
function* getCurrentUserSaga() {
  try {
    const user: User = yield call(authApi.getCurrentUser)
    yield put(authActions.getCurrentUserSuccess(user))
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : 'Failed to get user info'
    yield put(authActions.getCurrentUserFailure(errorMessage))
  }
}

/**
 * Change password saga
 */
function* changePasswordSaga(action: PayloadAction<ChangePasswordData>) {
  try {
    yield call(authApi.changePassword, action.payload)
    yield put(authActions.changePasswordSuccess())
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : 'Password change failed'
    yield put(authActions.changePasswordFailure(errorMessage))
  }
}

/**
 * Logout saga
 */
function* logoutSaga() {
  try {
    // Call logout endpoint (for server-side cleanup if needed)
    yield call(authApi.logout)
  } catch {
    // Ignore logout errors
  } finally {
    // Clear auth tokens regardless of API call result
    yield call(clearAuthTokens)
  }
}

/**
 * Root auth saga
 */
export function* authSagas() {
  yield takeLatest(authActions.loginRequest.type, loginSaga)
  yield takeLatest(authActions.registerRequest.type, registerSaga)
  yield takeLatest(authActions.refreshTokenRequest.type, refreshTokenSaga)
  yield takeLatest(authActions.getCurrentUserRequest.type, getCurrentUserSaga)
  yield takeLatest(authActions.changePasswordRequest.type, changePasswordSaga)
  yield takeLatest(authActions.logout.type, logoutSaga)
}
