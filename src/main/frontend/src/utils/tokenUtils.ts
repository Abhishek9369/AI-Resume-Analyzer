/**
 * JWT Token utility functions
 */

import type { TokenPayload } from '@/types/auth'

/**
 * Decode JWT token without verification
 */
export const decodeToken = (token: string): TokenPayload | null => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload) as TokenPayload
  } catch {
    return null
  }
}

/**
 * Check if token is expired
 */
export const isTokenExpired = (token: string): boolean => {
  const decoded = decodeToken(token)
  if (!decoded) return true

  const currentTime = Date.now() / 1000
  return decoded.exp < currentTime
}

/**
 * Get token expiration time in milliseconds
 */
export const getTokenExpiration = (token: string): number | null => {
  const decoded = decodeToken(token)
  if (!decoded) return null

  return decoded.exp * 1000
}

/**
 * Check if token will expire within the given minutes
 */
export const willTokenExpireSoon = (
  token: string,
  minutesThreshold: number = 5
): boolean => {
  const decoded = decodeToken(token)
  if (!decoded) return true

  const currentTime = Date.now() / 1000
  const thresholdTime = currentTime + minutesThreshold * 60

  return decoded.exp < thresholdTime
}

/**
 * Extract user role from token
 */
export const getRoleFromToken = (token: string): string | null => {
  const decoded = decodeToken(token)
  return decoded?.role || null
}

/**
 * Extract username from token
 */
export const getUsernameFromToken = (token: string): string | null => {
  const decoded = decodeToken(token)
  return decoded?.sub || null
}
