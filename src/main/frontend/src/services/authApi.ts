/**
 * Authentication API Service
 * Handles REST API calls for authentication endpoints
 */

import { axiosInstance } from './axiosInstance'
import type {
  LoginCredentials,
  RegisterData,
  ChangePasswordData,
  AuthResponse,
  User,
} from '@/types/auth'

const AUTH_BASE_URL = '/api/auth'

export const authApi = {
  /**
   * Login user with credentials
   */
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await axiosInstance.post(
      `${AUTH_BASE_URL}/login`,
      credentials
    )
    const data = response.data
    // Normalize flat backend response into the expected AuthResponse shape
    if (data && !data.user && data.username) {
      data.user = {
        id: data.userId || '',
        username: data.username || '',
        email: data.email || '',
        fullName: data.fullName || data.username || '',
        role: data.role || 'RECRUITER',
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    }
    return data as AuthResponse
  },

  /**
   * Register new user (admin only)
   */
  register: async (data: RegisterData): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(
      `${AUTH_BASE_URL}/register`,
      data
    )
    return response.data
  },

  /**
   * Refresh access token using refresh token
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await axiosInstance.post<AuthResponse>(
      `${AUTH_BASE_URL}/refresh`,
      { refreshToken }
    )
    return response.data
  },

  /**
   * Get current user info
   */
  getCurrentUser: async (): Promise<User> => {
    const response = await axiosInstance.get<User>(`${AUTH_BASE_URL}/me`)
    return response.data
  },

  /**
   * Validate current token
   */
  validateToken: async (): Promise<boolean> => {
    try {
      const response = await axiosInstance.post<{ valid: boolean }>(
        `${AUTH_BASE_URL}/validate`
      )
      return response.data.valid
    } catch {
      return false
    }
  },

  /**
   * Change password for current user
   */
  changePassword: async (data: ChangePasswordData): Promise<void> => {
    await axiosInstance.post(`${AUTH_BASE_URL}/change-password`, data)
  },

  /**
   * Logout current user (client-side token cleanup)
   */
  logout: async (): Promise<void> => {
    await axiosInstance.post(`${AUTH_BASE_URL}/logout`)
  },
}
