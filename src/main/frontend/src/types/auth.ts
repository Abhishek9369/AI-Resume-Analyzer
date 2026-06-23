/**
 * Authentication type definitions
 */

export enum UserRole {
  ADMIN = 'ADMIN',
  RECRUITER = 'RECRUITER',
  HR = 'HR',
  HIRING_MANAGER = 'HIRING_MANAGER',
}

export interface User {
  id: string
  username: string
  email: string
  fullName: string
  role: UserRole
  isActive: boolean
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface RegisterData {
  username: string
  password: string
  email: string
  fullName: string
  role: UserRole
}

export interface ChangePasswordData {
  oldPassword: string
  newPassword: string
}

export interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
  tokenExpiresAt: number | null
}

export interface TokenPayload {
  sub: string
  role: UserRole
  exp: number
  iat: number
}
